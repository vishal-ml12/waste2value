package com.waste2value.matching;

import com.waste2value.address.Address;
import com.waste2value.address.AddressRepository;
import com.waste2value.category.Category;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.matching.dto.CollectorRecommendationResponse;
import com.waste2value.matching.dto.ListingRecommendationResponse;
import com.waste2value.matching.dto.ProcessorRecommendationResponse;
import com.waste2value.pickup.PickupRequest;
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
import com.waste2value.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @Mock
    private ProcessorCapabilityRepository processorCapabilityRepository;

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private AddressRepository addressRepository;

    @Spy
    private DistanceCalculator distanceCalculator = new DistanceCalculator();

    @Spy
    private MatchingScoreCalculator scoreCalculator = new MatchingScoreCalculator(new MatchingConfiguration());

    @InjectMocks
    private MatchingService matchingService;

    private User generatorUser;
    private User collectorUser1;
    private User collectorUser2;
    private User processorUser;
    private Category category;
    private Address listingAddress;
    private Address collectorAddress1;
    private Listing postedListing;
    private CollectorProfile verifiedCollector1;
    private CollectorProfile unverifiedCollector;
    private ProcessorProfile verifiedProcessor;
    private ProcessorCapability capability;

    @BeforeEach
    void setUp() {
        generatorUser = new User();
        generatorUser.setId(2L);
        generatorUser.setRole(Role.GENERATOR);
        generatorUser.setFirstName("Eco");
        generatorUser.setLastName("Generator");
        generatorUser.setStatus(AccountStatus.ACTIVE);

        collectorUser1 = new User();
        collectorUser1.setId(3L);
        collectorUser1.setRole(Role.COLLECTOR);
        collectorUser1.setFirstName("City");
        collectorUser1.setLastName("Collector");
        collectorUser1.setStatus(AccountStatus.ACTIVE);

        collectorUser2 = new User();
        collectorUser2.setId(33L);
        collectorUser2.setRole(Role.COLLECTOR);
        collectorUser2.setFirstName("Other");
        collectorUser2.setLastName("Collector");
        collectorUser2.setStatus(AccountStatus.ACTIVE);

        processorUser = new User();
        processorUser.setId(4L);
        processorUser.setRole(Role.PROCESSOR);
        processorUser.setFirstName("Poly");
        processorUser.setLastName("Refining");
        processorUser.setStatus(AccountStatus.ACTIVE);

        category = new Category();
        category.setId(8L);
        category.setName("PET Bottles");
        category.setSlug("pet-bottles");
        category.setUnit("kg");

        listingAddress = new Address();
        listingAddress.setId(10L);
        listingAddress.setCity("Mumbai");
        listingAddress.setState("Maharashtra");
        listingAddress.setLatitude(new BigDecimal("19.0760"));
        listingAddress.setLongitude(new BigDecimal("72.8777"));

        collectorAddress1 = new Address();
        collectorAddress1.setId(20L);
        collectorAddress1.setCity("Mumbai");
        collectorAddress1.setState("Maharashtra");
        collectorAddress1.setLatitude(new BigDecimal("19.1000")); // ~3 km away
        collectorAddress1.setLongitude(new BigDecimal("72.8800"));

        postedListing = new Listing();
        postedListing.setId(100L);
        postedListing.setGenerator(generatorUser);
        postedListing.setCategory(category);
        postedListing.setAddress(listingAddress);
        postedListing.setTitle("PET Bottles Bulk");
        postedListing.setQuantity(new BigDecimal("200.00"));
        postedListing.setUnit("kg");
        postedListing.setExpectedPrice(new BigDecimal("18.00"));
        postedListing.setStatus(ListingStatus.POSTED);

        verifiedCollector1 = new CollectorProfile();
        verifiedCollector1.setId(1L);
        verifiedCollector1.setUser(collectorUser1);
        verifiedCollector1.setVerified(true);
        verifiedCollector1.setVehicleType("Mini Truck");
        verifiedCollector1.setServiceArea("Mumbai");
        verifiedCollector1.setMaxDistanceKm(new BigDecimal("50.00"));
        verifiedCollector1.setTotalPickups(15);
        verifiedCollector1.setAvgRating(new BigDecimal("4.80"));

        unverifiedCollector = new CollectorProfile();
        unverifiedCollector.setId(2L);
        unverifiedCollector.setUser(collectorUser2);
        unverifiedCollector.setVerified(false);

        verifiedProcessor = new ProcessorProfile();
        verifiedProcessor.setId(10L);
        verifiedProcessor.setUser(processorUser);
        verifiedProcessor.setCompanyName("Eco Polymer Plant");
        verifiedProcessor.setLicenseNumber("LIC-9988");
        verifiedProcessor.setVerified(true);
        verifiedProcessor.setMaxCapacityKg(new BigDecimal("5000.00"));
        verifiedProcessor.setCurrentLoadKg(new BigDecimal("1000.00")); // 4000 kg available
        verifiedProcessor.setAvgRating(new BigDecimal("4.90"));

        capability = new ProcessorCapability();
        capability.setId(50L);
        capability.setProcessor(verifiedProcessor);
        capability.setCategory(category);
        capability.setMinQuantity(new BigDecimal("50.00"));
        capability.setMaxQuantity(new BigDecimal("1000.00"));
        capability.setPricePerUnit(new BigDecimal("22.00"));
        capability.setActive(true);
    }

    @Test
    @DisplayName("Test 1 & 2: Verified collector is eligible, unverified collector is excluded")
    void verifiedCollectorIncludedUnverifiedExcluded() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));
        when(addressRepository.findByUserIdAndIsDefaultTrue(3L)).thenReturn(Optional.of(collectorAddress1));
        when(pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(eq(100L), eq(3L), any()))
                .thenReturn(Optional.empty());

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).collectorId()).isEqualTo(1L);
        assertThat(recommendations.get(0).isVerified()).isTrue();
    }

    @Test
    @DisplayName("Test 3: Inactive account is excluded from collector recommendations")
    void inactiveAccountExcluded() {
        collectorUser1.setStatus(AccountStatus.SUSPENDED);

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 4: Generator cannot be recommended as collector for own listing")
    void generatorCannotBeRecommendedForOwnListing() {
        verifiedCollector1.setUser(generatorUser); // Same user

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 5: Maximum distance filtering excludes collectors beyond radius")
    void maxDistanceFiltersOutDistantCollector() {
        // Collector is in Pune (lat 18.5204, lon 73.8567) ~120 km from Mumbai
        Address distantAddress = new Address();
        distantAddress.setLatitude(new BigDecimal("18.5204"));
        distantAddress.setLongitude(new BigDecimal("73.8567"));

        verifiedCollector1.setMaxDistanceKm(new BigDecimal("40.00")); // Only travels up to 40 km

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));
        when(addressRepository.findByUserIdAndIsDefaultTrue(3L)).thenReturn(Optional.of(distantAddress));
        when(pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(eq(100L), eq(3L), any()))
                .thenReturn(Optional.empty());

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 6: Missing coordinates handled safely with distance marked unavailable")
    void missingCoordinatesMarkedUnavailable() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));
        when(addressRepository.findByUserIdAndIsDefaultTrue(3L)).thenReturn(Optional.empty());
        when(addressRepository.findByUserIdOrderByCreatedAtDesc(3L)).thenReturn(List.of());
        when(pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(eq(100L), eq(3L), any()))
                .thenReturn(Optional.empty());

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).isDistanceAvailable()).isFalse();
        assertThat(recommendations.get(0).distanceKm()).isNull();
        assertThat(recommendations.get(0).suitabilityScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Test 7: Prevents duplicate recommendation when active pickup request exists")
    void preventsDuplicateActivePickupRequest() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedCollector1)));

        PickupRequest activeRequest = new PickupRequest();
        activeRequest.setStatus(PickupRequestStatus.REQUESTED);
        when(pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(eq(100L), eq(3L), any()))
                .thenReturn(Optional.of(activeRequest));

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 8 & 9: Processor capability and capacity eligibility matches suitable processor")
    void processorCapabilityAndCapacityEligibility() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(processorCapabilityRepository.findByCategoryIdAndIsActiveTrue(8L))
                .thenReturn(List.of(capability));
        when(addressRepository.findByUserIdAndIsDefaultTrue(4L)).thenReturn(Optional.empty());
        when(addressRepository.findByUserIdOrderByCreatedAtDesc(4L)).thenReturn(List.of());

        List<ProcessorRecommendationResponse> recommendations =
                matchingService.getRecommendedProcessorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).processorId()).isEqualTo(10L);
        assertThat(recommendations.get(0).availableCapacityKg()).isEqualTo(new BigDecimal("4000.00"));
        assertThat(recommendations.get(0).suitabilityScore()).isGreaterThan(70.0);
    }

    @Test
    @DisplayName("Test 10: Insufficient processor capacity excludes facility")
    void insufficientCapacityExcludesFacility() {
        // Current load is 4900 kg on 5000 kg max -> only 100 kg available, but listing is 200 kg
        verifiedProcessor.setCurrentLoadKg(new BigDecimal("4900.00"));

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(processorCapabilityRepository.findByCategoryIdAndIsActiveTrue(8L))
                .thenReturn(List.of(capability));

        List<ProcessorRecommendationResponse> recommendations =
                matchingService.getRecommendedProcessorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 11: Quantity limit violation excludes facility")
    void quantityLimitViolationExcludesFacility() {
        // Capability requires minimum 500 kg, but listing is only 200 kg
        capability.setMinQuantity(new BigDecimal("500.00"));

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(processorCapabilityRepository.findByCategoryIdAndIsActiveTrue(8L))
                .thenReturn(List.of(capability));

        List<ProcessorRecommendationResponse> recommendations =
                matchingService.getRecommendedProcessorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 12 & 13: Sorting by highest suitability score ranks best match first")
    void sortingRanksHighestScoreFirst() {
        CollectorProfile highScoring = verifiedCollector1; // 15 pickups, 4.8 rating

        CollectorProfile lowerScoring = new CollectorProfile();
        lowerScoring.setId(99L);
        lowerScoring.setUser(collectorUser2);
        lowerScoring.setVerified(true);
        lowerScoring.setVehicleType("Three Wheeler");
        lowerScoring.setServiceArea("Other Region");
        lowerScoring.setMaxDistanceKm(new BigDecimal("50.00"));
        lowerScoring.setTotalPickups(0);
        lowerScoring.setAvgRating(BigDecimal.ZERO);

        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lowerScoring, highScoring)));
        when(pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).collectorId()).isEqualTo(1L); // High scoring first
        assertThat(recommendations.get(0).suitabilityScore())
                .isGreaterThan(recommendations.get(1).suitabilityScore());
    }

    @Test
    @DisplayName("Test 14: Cross-user authorization guard: Generator cannot view another user's listing recommendations")
    void generatorCannotViewAnotherUserListingRecommendations() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));

        // User id 99 attempts to view recommendations for listing owned by User id 2
        assertThatThrownBy(() -> matchingService.getRecommendedCollectorsForListing(99L, Role.GENERATOR, 100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized to view recommendations for this listing");
    }

    @Test
    @DisplayName("Test 15: Unverified collector cannot view personalized recommended listings")
    void unverifiedCollectorCannotViewRecommendations() {
        when(collectorProfileRepository.findByUserId(33L)).thenReturn(Optional.of(unverifiedCollector));

        assertThatThrownBy(() -> matchingService.getRecommendedListingsForCollector(33L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("must be verified by an administrator");
    }

    @Test
    @DisplayName("Test 16: Empty recommendation results when no candidates exist")
    void emptyRecommendationResults() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));
        when(collectorProfileRepository.findByIsVerifiedTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Test 17: Non-POSTED listing returns empty recommendation results")
    void nonPostedListingReturnsEmptyRecommendations() {
        postedListing.setStatus(ListingStatus.COMPLETED);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));

        List<CollectorRecommendationResponse> recommendations =
                matchingService.getRecommendedCollectorsForListing(2L, Role.GENERATOR, 100L);

        assertThat(recommendations).isEmpty();
    }
}
