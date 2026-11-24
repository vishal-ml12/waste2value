package com.waste2value.matching;

import com.waste2value.address.Address;
import com.waste2value.address.AddressRepository;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.matching.dto.CollectorRecommendationResponse;
import com.waste2value.matching.dto.ListingRecommendationResponse;
import com.waste2value.matching.dto.ProcessorRecommendationResponse;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class MatchingService {

    private final ListingRepository listingRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final ProcessorProfileRepository processorProfileRepository;
    private final ProcessorCapabilityRepository processorCapabilityRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final AddressRepository addressRepository;
    private final DistanceCalculator distanceCalculator;
    private final MatchingScoreCalculator scoreCalculator;

    public MatchingService(
            ListingRepository listingRepository,
            CollectorProfileRepository collectorProfileRepository,
            ProcessorProfileRepository processorProfileRepository,
            ProcessorCapabilityRepository processorCapabilityRepository,
            PickupRequestRepository pickupRequestRepository,
            AddressRepository addressRepository,
            DistanceCalculator distanceCalculator,
            MatchingScoreCalculator scoreCalculator
    ) {
        this.listingRepository = listingRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.processorProfileRepository = processorProfileRepository;
        this.processorCapabilityRepository = processorCapabilityRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.addressRepository = addressRepository;
        this.distanceCalculator = distanceCalculator;
        this.scoreCalculator = scoreCalculator;
    }

    /**
     * Recommends eligible, top-ranked collectors for a given waste listing.
     * Accessible by the listing owner or administrator.
     */
    public List<CollectorRecommendationResponse> getRecommendedCollectorsForListing(Long userId, Role role, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (role != Role.ADMIN && !listing.getGenerator().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view recommendations for this listing");
        }

        // Hard Filter: Only POSTED listings accept collector matches
        if (listing.getStatus() != ListingStatus.POSTED) {
            return Collections.emptyList();
        }

        List<CollectorProfile> verifiedCollectors = collectorProfileRepository.findByIsVerifiedTrue(Pageable.unpaged()).getContent();
        List<CollectorRecommendationResponse> candidates = new ArrayList<>();

        for (CollectorProfile collector : verifiedCollectors) {
            // Stage A Hard Filter 1: Active user account
            if (collector.getUser().getStatus() != AccountStatus.ACTIVE) {
                continue;
            }

            // Stage A Hard Filter 2: Generator cannot be recommended as collector
            if (collector.getUser().getId().equals(listing.getGenerator().getId())) {
                continue;
            }

            // Stage A Hard Filter 3: Prevent duplicate active pickup requests
            boolean hasActiveRequest = pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(
                    listingId,
                    collector.getUser().getId(),
                    List.of(PickupRequestStatus.REQUESTED, PickupRequestStatus.ACCEPTED)
            ).isPresent();
            if (hasActiveRequest) {
                continue;
            }

            // Stage A Hard Filter 4: Maximum Distance check (if coordinates available)
            Address collectorAddress = getOperatingAddress(collector.getUser().getId()).orElse(null);
            Double distanceKm = null;
            boolean isDistanceAvailable = false;

            if (collectorAddress != null && listing.getAddress() != null) {
                distanceKm = distanceCalculator.calculateDistanceKm(
                        listing.getAddress().getLatitude(),
                        listing.getAddress().getLongitude(),
                        collectorAddress.getLatitude(),
                        collectorAddress.getLongitude()
                );
                if (distanceKm != null) {
                    isDistanceAvailable = true;
                    // Strict Distance Filter
                    if (collector.getMaxDistanceKm() != null && collector.getMaxDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
                        if (distanceKm > collector.getMaxDistanceKm().doubleValue()) {
                            continue; // Out of range
                        }
                    }
                }
            }

            // Stage B: Calculate Suitability Score
            MatchingScoreCalculator.ScoreResult scoreResult = scoreCalculator.calculateCollectorScore(collector, listing, distanceKm);

            String name = collector.getUser().getFirstName() + " " + collector.getUser().getLastName();
            candidates.add(new CollectorRecommendationResponse(
                    collector.getId(),
                    collector.getUser().getId(),
                    name,
                    collector.getVehicleType(),
                    collector.getServiceArea(),
                    collector.getMaxDistanceKm(),
                    collector.isVerified(),
                    collector.getAvgRating(),
                    collector.getTotalPickups(),
                    distanceKm,
                    isDistanceAvailable,
                    scoreResult.totalScore(),
                    scoreResult.reasons()
            ));
        }

        // Rank by highest suitability score
        candidates.sort(Comparator.comparingDouble(CollectorRecommendationResponse::suitabilityScore).reversed());
        return candidates;
    }

    /**
     * Recommends eligible, top-ranked processors for a given waste listing.
     * Accessible by the listing owner, allocated collector, or administrator.
     */
    public List<ProcessorRecommendationResponse> getRecommendedProcessorsForListing(Long userId, Role role, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        boolean isAuthorized = role == Role.ADMIN
                || listing.getGenerator().getId().equals(userId)
                || pickupRequestRepository.findAcceptedRequestByListingId(listingId)
                .map(pr -> pr.getCollector().getId().equals(userId))
                .orElse(false);

        if (!isAuthorized) {
            throw new ForbiddenException("You are not authorized to view processor recommendations for this listing");
        }

        // Hard Filter: Must be in POSTED or ALLOCATED status
        if (listing.getStatus() != ListingStatus.POSTED && listing.getStatus() != ListingStatus.ALLOCATED) {
            return Collections.emptyList();
        }

        // Find active capabilities for the material category
        List<ProcessorCapability> capabilities = processorCapabilityRepository.findByCategoryIdAndIsActiveTrue(listing.getCategory().getId());
        List<ProcessorRecommendationResponse> candidates = new ArrayList<>();

        for (ProcessorCapability cap : capabilities) {
            ProcessorProfile processor = cap.getProcessor();

            // Stage A Hard Filter 1: Must be verified processor with active user account
            if (!processor.isVerified() || processor.getUser().getStatus() != AccountStatus.ACTIVE) {
                continue;
            }

            // Stage A Hard Filter 2: Cannot recommend listing generator
            if (processor.getUser().getId().equals(listing.getGenerator().getId())) {
                continue;
            }

            // Stage A Hard Filter 3: Quantity limits
            BigDecimal qty = listing.getQuantity();
            if (cap.getMinQuantity().compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(cap.getMinQuantity()) < 0) {
                continue;
            }
            if (cap.getMaxQuantity().compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(cap.getMaxQuantity()) > 0) {
                continue;
            }

            // Stage A Hard Filter 4: Available capacity check
            BigDecimal maxCap = processor.getMaxCapacityKg();
            BigDecimal curLoad = processor.getCurrentLoadKg();
            BigDecimal availableCap = maxCap.subtract(curLoad);
            if (availableCap.compareTo(BigDecimal.ZERO) < 0) availableCap = BigDecimal.ZERO;

            if (maxCap.compareTo(BigDecimal.ZERO) > 0 && availableCap.compareTo(qty) < 0) {
                continue; // Facility does not have sufficient remaining headroom
            }

            // Location & Distance
            Address procAddress = getOperatingAddress(processor.getUser().getId()).orElse(null);
            Double distanceKm = null;
            boolean isDistanceAvailable = false;

            if (procAddress != null && listing.getAddress() != null) {
                distanceKm = distanceCalculator.calculateDistanceKm(
                        listing.getAddress().getLatitude(),
                        listing.getAddress().getLongitude(),
                        procAddress.getLatitude(),
                        procAddress.getLongitude()
                );
                if (distanceKm != null) {
                    isDistanceAvailable = true;
                }
            }

            // Stage B: Calculate Suitability Score
            MatchingScoreCalculator.ScoreResult scoreResult = scoreCalculator.calculateProcessorScore(processor, cap, listing, distanceKm);

            candidates.add(new ProcessorRecommendationResponse(
                    processor.getId(),
                    processor.getUser().getId(),
                    processor.getCompanyName(),
                    processor.getLicenseNumber(),
                    processor.isVerified(),
                    processor.getAvgRating(),
                    processor.getMaxCapacityKg(),
                    processor.getCurrentLoadKg(),
                    availableCap,
                    cap.getCategory().getName(),
                    cap.getMinQuantity(),
                    cap.getMaxQuantity(),
                    cap.getPricePerUnit(),
                    distanceKm,
                    isDistanceAvailable,
                    scoreResult.totalScore(),
                    scoreResult.reasons()
            ));
        }

        // Rank by highest suitability score
        candidates.sort(Comparator.comparingDouble(ProcessorRecommendationResponse::suitabilityScore).reversed());
        return candidates;
    }

    /**
     * Recommends eligible POSTED waste listings tailored to the authenticated collector.
     */
    public List<ListingRecommendationResponse> getRecommendedListingsForCollector(Long collectorUserId) {
        CollectorProfile collector = collectorProfileRepository.findByUserId(collectorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector profile not found for current user"));

        if (!collector.isVerified()) {
            throw new ForbiddenException("Your collector profile must be verified by an administrator to view personalized recommendations");
        }

        Address collectorAddress = getOperatingAddress(collectorUserId).orElse(null);

        // Fetch all POSTED listings
        List<Listing> postedListings = listingRepository.findAll().stream()
                .filter(l -> l.getStatus() == ListingStatus.POSTED)
                .toList();

        List<ListingRecommendationResponse> candidates = new ArrayList<>();

        for (Listing listing : postedListings) {
            // Hard Filter 1: Collector cannot collect own listings
            if (listing.getGenerator().getId().equals(collectorUserId)) {
                continue;
            }

            // Hard Filter 2: Prevent duplicate active requests
            boolean hasActive = pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(
                    listing.getId(),
                    collectorUserId,
                    List.of(PickupRequestStatus.REQUESTED, PickupRequestStatus.ACCEPTED)
            ).isPresent();
            if (hasActive) {
                continue;
            }

            // Distance calculation and filter
            Double distanceKm = null;
            boolean isDistanceAvailable = false;

            if (collectorAddress != null && listing.getAddress() != null) {
                distanceKm = distanceCalculator.calculateDistanceKm(
                        listing.getAddress().getLatitude(),
                        listing.getAddress().getLongitude(),
                        collectorAddress.getLatitude(),
                        collectorAddress.getLongitude()
                );
                if (distanceKm != null) {
                    isDistanceAvailable = true;
                    if (collector.getMaxDistanceKm() != null && collector.getMaxDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
                        if (distanceKm > collector.getMaxDistanceKm().doubleValue()) {
                            continue; // Outside collector's max operating radius
                        }
                    }
                }
            }

            // Calculate score
            MatchingScoreCalculator.ScoreResult scoreResult = scoreCalculator.calculateCollectorScore(collector, listing, distanceKm);

            String genName = listing.getGenerator().getFirstName() + " " + listing.getGenerator().getLastName();
            String city = listing.getAddress() != null ? listing.getAddress().getCity() : "";
            String state = listing.getAddress() != null ? listing.getAddress().getState() : "";

            candidates.add(new ListingRecommendationResponse(
                    listing.getId(),
                    listing.getTitle(),
                    listing.getCategory().getId(),
                    listing.getCategory().getName(),
                    listing.getCategory().getSlug(),
                    listing.getQuantity(),
                    listing.getUnit(),
                    listing.getExpectedPrice(),
                    listing.getGenerator().getId(),
                    genName,
                    city,
                    state,
                    distanceKm,
                    isDistanceAvailable,
                    scoreResult.totalScore(),
                    scoreResult.reasons(),
                    listing.getCreatedAt()
            ));
        }

        candidates.sort(Comparator.comparingDouble(ListingRecommendationResponse::suitabilityScore).reversed());
        return candidates;
    }

    /**
     * Recommends eligible POSTED waste listings tailored to the authenticated processor facility.
     */
    public List<ListingRecommendationResponse> getRecommendedListingsForProcessor(Long processorUserId) {
        ProcessorProfile processor = processorProfileRepository.findByUserId(processorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        if (!processor.isVerified()) {
            throw new ForbiddenException("Your processor facility profile must be verified by an administrator to view personalized recommendations");
        }

        List<ProcessorCapability> activeCaps = processorCapabilityRepository.findByProcessorId(processor.getId())
                .stream()
                .filter(ProcessorCapability::isActive)
                .toList();

        if (activeCaps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProcessorCapability> capabilityMap = new HashMap<>();
        for (ProcessorCapability cap : activeCaps) {
            capabilityMap.put(cap.getCategory().getId(), cap);
        }

        Address procAddress = getOperatingAddress(processorUserId).orElse(null);

        // Fetch all POSTED listings
        List<Listing> postedListings = listingRepository.findAll().stream()
                .filter(l -> l.getStatus() == ListingStatus.POSTED)
                .toList();

        List<ListingRecommendationResponse> candidates = new ArrayList<>();

        for (Listing listing : postedListings) {
            // Hard Filter 1: Processor cannot be the listing generator
            if (listing.getGenerator().getId().equals(processorUserId)) {
                continue;
            }

            // Hard Filter 2: Must match an active capability category
            ProcessorCapability cap = capabilityMap.get(listing.getCategory().getId());
            if (cap == null) {
                continue;
            }

            // Hard Filter 3: Min/max quantity limits
            BigDecimal qty = listing.getQuantity();
            if (cap.getMinQuantity().compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(cap.getMinQuantity()) < 0) {
                continue;
            }
            if (cap.getMaxQuantity().compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(cap.getMaxQuantity()) > 0) {
                continue;
            }

            // Hard Filter 4: Sufficient available facility capacity
            BigDecimal maxCap = processor.getMaxCapacityKg();
            BigDecimal curLoad = processor.getCurrentLoadKg();
            BigDecimal availableCap = maxCap.subtract(curLoad);
            if (availableCap.compareTo(BigDecimal.ZERO) < 0) availableCap = BigDecimal.ZERO;

            if (maxCap.compareTo(BigDecimal.ZERO) > 0 && availableCap.compareTo(qty) < 0) {
                continue;
            }

            // Distance
            Double distanceKm = null;
            boolean isDistanceAvailable = false;
            if (procAddress != null && listing.getAddress() != null) {
                distanceKm = distanceCalculator.calculateDistanceKm(
                        listing.getAddress().getLatitude(),
                        listing.getAddress().getLongitude(),
                        procAddress.getLatitude(),
                        procAddress.getLongitude()
                );
                if (distanceKm != null) {
                    isDistanceAvailable = true;
                }
            }

            MatchingScoreCalculator.ScoreResult scoreResult = scoreCalculator.calculateProcessorScore(processor, cap, listing, distanceKm);

            String genName = listing.getGenerator().getFirstName() + " " + listing.getGenerator().getLastName();
            String city = listing.getAddress() != null ? listing.getAddress().getCity() : "";
            String state = listing.getAddress() != null ? listing.getAddress().getState() : "";

            candidates.add(new ListingRecommendationResponse(
                    listing.getId(),
                    listing.getTitle(),
                    listing.getCategory().getId(),
                    listing.getCategory().getName(),
                    listing.getCategory().getSlug(),
                    listing.getQuantity(),
                    listing.getUnit(),
                    listing.getExpectedPrice(),
                    listing.getGenerator().getId(),
                    genName,
                    city,
                    state,
                    distanceKm,
                    isDistanceAvailable,
                    scoreResult.totalScore(),
                    scoreResult.reasons(),
                    listing.getCreatedAt()
            ));
        }

        candidates.sort(Comparator.comparingDouble(ListingRecommendationResponse::suitabilityScore).reversed());
        return candidates;
    }

    private Optional<Address> getOperatingAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .or(() -> addressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().findFirst());
    }
}
