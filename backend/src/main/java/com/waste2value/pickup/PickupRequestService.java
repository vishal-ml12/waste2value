package com.waste2value.pickup;

import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.pickup.dto.CompletePickupRequest;
import com.waste2value.pickup.dto.CreatePickupRequest;
import com.waste2value.pickup.dto.PickupRequestResponse;
import com.waste2value.pickup.dto.RespondPickupRequest;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.*;
import com.waste2value.transaction.TransactionService;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PickupRequestService {

    private final PickupRequestRepository pickupRequestRepository;
    private final ListingRepository listingRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public PickupRequestService(
            PickupRequestRepository pickupRequestRepository,
            ListingRepository listingRepository,
            CollectorProfileRepository collectorProfileRepository,
            UserRepository userRepository,
            TransactionService transactionService,
            NotificationService notificationService
    ) {
        this.pickupRequestRepository = pickupRequestRepository;
        this.listingRepository = listingRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PickupRequestResponse createPickupRequest(Long collectorUserId, Long listingId, CreatePickupRequest request) {
        User collector = userRepository.findById(collectorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + collectorUserId));

        if (collector.getRole() != Role.COLLECTOR) {
            throw new ForbiddenException("Only users with role COLLECTOR can request pickups");
        }

        CollectorProfile profile = collectorProfileRepository.findByUserId(collectorUserId)
                .orElseThrow(() -> new ForbiddenException("You must complete your collector profile before requesting pickups"));

        if (!profile.isVerified()) {
            throw new ForbiddenException("Your collector profile is pending admin verification. Only verified collectors can request pickups.");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (listing.getStatus() != ListingStatus.POSTED) {
            throw new BadRequestException("Cannot request pickup for listing with status: " + listing.getStatus() + ". Only POSTED listings accept requests.");
        }

        if (listing.getGenerator().getId().equals(collectorUserId)) {
            throw new BadRequestException("A waste generator cannot request pickup for their own listing");
        }

        var activeStatuses = List.of(PickupRequestStatus.REQUESTED, PickupRequestStatus.ACCEPTED);
        if (pickupRequestRepository.findByListingIdAndCollectorIdAndStatusIn(listingId, collectorUserId, activeStatuses).isPresent()) {
            throw new ConflictException("You already have an active pickup request for this listing");
        }

        if (request.proposedPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Proposed price cannot be negative");
        }

        PickupRequest pickupRequest = new PickupRequest();
        pickupRequest.setListing(listing);
        pickupRequest.setCollector(collector);
        pickupRequest.setStatus(PickupRequestStatus.REQUESTED);
        pickupRequest.setProposedPrice(request.proposedPrice());
        pickupRequest.setNote(request.note());
        pickupRequest.setRequestedAt(Instant.now());

        pickupRequest = pickupRequestRepository.save(pickupRequest);

        // Notify Generator
        String collectorName = collector.getFirstName() + " " + collector.getLastName();
        notificationService.sendNotification(
                listing.getGenerator(),
                NotificationType.PICKUP_REQUESTED,
                "New Pickup Request Received",
                "Collector " + collectorName + " requested pickup for your listing '" + listing.getTitle() + "' offering ₹" + request.proposedPrice() + "/" + listing.getUnit(),
                "PICKUP_REQUEST",
                pickupRequest.getId()
        );

        return PickupRequestResponse.from(pickupRequest);
    }

    @Transactional(readOnly = true)
    public List<PickupRequestResponse> getListingRequests(Long userId, Role role, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (role != Role.ADMIN && !listing.getGenerator().getId().equals(userId)) {
            throw new ForbiddenException("Only the listing owner or an administrator can view requests for this listing");
        }

        return pickupRequestRepository.findByListingId(listingId)
                .stream()
                .map(PickupRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PickupRequestResponse> getMyRequests(Long collectorUserId, PickupRequestStatus status, Pageable pageable) {
        if (status != null) {
            return PageResponse.from(pickupRequestRepository.findByCollectorIdAndStatus(collectorUserId, status, pageable).map(PickupRequestResponse::from));
        }
        return PageResponse.from(pickupRequestRepository.findByCollectorId(collectorUserId, pageable).map(PickupRequestResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<PickupRequestResponse> getReceivedRequestsForGenerator(Long generatorUserId, PickupRequestStatus status, Pageable pageable) {
        if (status != null) {
            return PageResponse.from(pickupRequestRepository.findByListingGeneratorIdAndStatus(generatorUserId, status, pageable).map(PickupRequestResponse::from));
        }
        return PageResponse.from(pickupRequestRepository.findByListingGeneratorId(generatorUserId, pageable).map(PickupRequestResponse::from));
    }

    @Transactional(readOnly = true)
    public PickupRequestResponse getRequestById(Long userId, Role role, Long id) {
        PickupRequest pr = pickupRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + id));

        boolean isAuthorized = role == Role.ADMIN
                || pr.getCollector().getId().equals(userId)
                || pr.getListing().getGenerator().getId().equals(userId);

        if (!isAuthorized) {
            throw new ForbiddenException("You are not authorized to view this pickup request");
        }

        return PickupRequestResponse.from(pr);
    }

    @Transactional
    public PickupRequestResponse respondToRequest(Long generatorUserId, Long requestId, RespondPickupRequest request) {
        PickupRequest pickupRequest = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + requestId));

        Listing listing = pickupRequest.getListing();

        if (!listing.getGenerator().getId().equals(generatorUserId)) {
            throw new ForbiddenException("Only the waste generator who created this listing can respond to pickup requests");
        }

        if (pickupRequest.getStatus() != PickupRequestStatus.REQUESTED) {
            throw new InvalidStateTransitionException("Cannot respond to pickup request in status: " + pickupRequest.getStatus());
        }

        if (Boolean.TRUE.equals(request.accept())) {
            // SAFE ALLOCATION CONCURRENCY: Acquire pessimistic write lock on listing
            Listing lockedListing = listingRepository.findByIdWithLock(listing.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

            if (lockedListing.getStatus() != ListingStatus.POSTED) {
                throw new ConflictException("Listing has already been allocated or is no longer posted (current status: " + lockedListing.getStatus() + ")");
            }

            // Accept selected request
            pickupRequest.setStatus(PickupRequestStatus.ACCEPTED);
            pickupRequest.setRespondedAt(Instant.now());
            if (request.note() != null) {
                pickupRequest.setCompletionNote("Response note: " + request.note());
            }

            // Allocate listing
            lockedListing.setStatus(ListingStatus.ALLOCATED);
            listingRepository.save(lockedListing);

            // Reject all other competing REQUESTED pickup requests for this listing
            List<PickupRequest> competingRequests = pickupRequestRepository.findByListingIdAndStatus(lockedListing.getId(), PickupRequestStatus.REQUESTED);
            for (PickupRequest competing : competingRequests) {
                if (!competing.getId().equals(pickupRequest.getId())) {
                    competing.setStatus(PickupRequestStatus.REJECTED);
                    competing.setRespondedAt(Instant.now());
                    competing.setCompletionNote("Listing was allocated to another collector.");
                    pickupRequestRepository.save(competing);

                    // Notify competing collector
                    notificationService.sendNotification(
                            competing.getCollector(),
                            NotificationType.PICKUP_REJECTED,
                            "Pickup Request Update",
                            "Your pickup request for '" + lockedListing.getTitle() + "' was not accepted as the listing was allocated to another collector.",
                            "PICKUP_REQUEST",
                            competing.getId()
                    );
                }
            }

            pickupRequest = pickupRequestRepository.save(pickupRequest);

            // Notify accepted collector
            notificationService.sendNotification(
                    pickupRequest.getCollector(),
                    NotificationType.PICKUP_ACCEPTED,
                    "Pickup Request Accepted! 🎉",
                    "Your pickup request for '" + lockedListing.getTitle() + "' was accepted at ₹" + pickupRequest.getProposedPrice() + "/" + lockedListing.getUnit() + ". Please coordinate pickup with the generator.",
                    "PICKUP_REQUEST",
                    pickupRequest.getId()
            );

            // Notify generator
            notificationService.sendNotification(
                    lockedListing.getGenerator(),
                    NotificationType.LISTING_ALLOCATED,
                    "Listing Allocated",
                    "Your listing '" + lockedListing.getTitle() + "' is now ALLOCATED to collector " + pickupRequest.getCollector().getFirstName() + " " + pickupRequest.getCollector().getLastName() + ".",
                    "LISTING",
                    lockedListing.getId()
            );

        } else {
            // Rejection
            pickupRequest.setStatus(PickupRequestStatus.REJECTED);
            pickupRequest.setRespondedAt(Instant.now());
            if (request.note() != null) {
                pickupRequest.setCompletionNote("Rejection reason: " + request.note());
            }
            pickupRequest = pickupRequestRepository.save(pickupRequest);

            notificationService.sendNotification(
                    pickupRequest.getCollector(),
                    NotificationType.PICKUP_REJECTED,
                    "Pickup Request Rejected",
                    "Your pickup request for '" + listing.getTitle() + "' was declined by the generator.",
                    "PICKUP_REQUEST",
                    pickupRequest.getId()
            );
        }

        return PickupRequestResponse.from(pickupRequest);
    }

    @Transactional
    public PickupRequestResponse cancelRequest(Long userId, Role role, Long requestId) {
        PickupRequest pickupRequest = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + requestId));

        boolean isCollector = pickupRequest.getCollector().getId().equals(userId);
        boolean isGenerator = pickupRequest.getListing().getGenerator().getId().equals(userId);
        boolean isAdmin = role == Role.ADMIN;

        if (!isCollector && !isGenerator && !isAdmin) {
            throw new ForbiddenException("You can only cancel pickup requests you participate in");
        }

        if (pickupRequest.getStatus() != PickupRequestStatus.REQUESTED && pickupRequest.getStatus() != PickupRequestStatus.ACCEPTED) {
            throw new InvalidStateTransitionException("Cannot cancel pickup request with status: " + pickupRequest.getStatus());
        }

        Listing listing = pickupRequest.getListing();

        // If the request was previously accepted, revert listing back to POSTED so others can request!
        if (pickupRequest.getStatus() == PickupRequestStatus.ACCEPTED) {
            Listing lockedListing = listingRepository.findByIdWithLock(listing.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

            if (lockedListing.getStatus() == ListingStatus.ALLOCATED) {
                lockedListing.setStatus(ListingStatus.POSTED);
                listingRepository.save(lockedListing);

                if (isCollector) {
                    notificationService.sendNotification(
                            lockedListing.getGenerator(),
                            NotificationType.PICKUP_CANCELLED,
                            "Collector Cancelled Accepted Pickup",
                            "Collector " + pickupRequest.getCollector().getFirstName() + " cancelled the allocated pickup for '" + lockedListing.getTitle() + "'. The listing has returned to POSTED status.",
                            "LISTING",
                            lockedListing.getId()
                    );
                } else {
                    notificationService.sendNotification(
                            pickupRequest.getCollector(),
                            NotificationType.PICKUP_CANCELLED,
                            "Generator Cancelled Accepted Pickup",
                            "The waste generator cancelled the allocated pickup for '" + lockedListing.getTitle() + "'. The listing has returned to POSTED status.",
                            "LISTING",
                            lockedListing.getId()
                    );
                }
            }
        }

        pickupRequest.setStatus(PickupRequestStatus.CANCELLED);
        pickupRequest = pickupRequestRepository.save(pickupRequest);

        return PickupRequestResponse.from(pickupRequest);
    }

    @Transactional
    public PickupRequestResponse cancelRequest(Long collectorUserId, Long requestId) {
        return cancelRequest(collectorUserId, Role.COLLECTOR, requestId);
    }

    @Transactional
    public PickupRequestResponse completePickup(Long collectorUserId, Long requestId, CompletePickupRequest request) {
        PickupRequest pickupRequest = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + requestId));

        if (!pickupRequest.getCollector().getId().equals(collectorUserId)) {
            throw new ForbiddenException("Only the allocated collector can complete the pickup");
        }

        if (pickupRequest.getStatus() != PickupRequestStatus.ACCEPTED) {
            throw new InvalidStateTransitionException("Cannot complete pickup request with status: " + pickupRequest.getStatus() + ". Must be ACCEPTED.");
        }

        Listing listing = pickupRequest.getListing();
        if (listing.getStatus() != ListingStatus.ALLOCATED) {
            throw new InvalidStateTransitionException("Associated listing is in status: " + listing.getStatus() + ". Expected ALLOCATED.");
        }

        if (request.actualQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Actual collected quantity must be greater than zero");
        }

        Instant now = Instant.now();
        pickupRequest.setActualQuantity(request.actualQuantity());
        pickupRequest.setCompletionNote(request.completionNote());
        pickupRequest.setCompletedAt(now);
        pickupRequest.setStatus(PickupRequestStatus.COMPLETED);
        pickupRequest = pickupRequestRepository.save(pickupRequest);

        // Complete listing
        listing.setStatus(ListingStatus.COMPLETED);
        listingRepository.save(listing);

        // Record financial transaction
        transactionService.createTransactionForPickup(pickupRequest, request.actualQuantity());

        // Increment collector pickup count
        collectorProfileRepository.findByUserId(collectorUserId).ifPresent(profile -> {
            profile.setTotalPickups(profile.getTotalPickups() + 1);
            collectorProfileRepository.save(profile);
        });

        // Notifications
        notificationService.sendNotification(
                listing.getGenerator(),
                NotificationType.PICKUP_COMPLETED,
                "Waste Pickup Completed! 📦",
                "Collector " + pickupRequest.getCollector().getFirstName() + " recorded completion of pickup for '" + listing.getTitle() + "' (Quantity: " + request.actualQuantity() + " " + listing.getUnit() + "). Transaction record created.",
                "PICKUP_REQUEST",
                pickupRequest.getId()
        );

        notificationService.sendNotification(
                pickupRequest.getCollector(),
                NotificationType.PICKUP_COMPLETED,
                "Pickup Completed",
                "You successfully completed pickup for '" + listing.getTitle() + "'. A transaction record of ₹" + request.actualQuantity().multiply(pickupRequest.getProposedPrice()) + " has been logged.",
                "PICKUP_REQUEST",
                pickupRequest.getId()
        );

        return PickupRequestResponse.from(pickupRequest);
    }

    @Transactional(readOnly = true)
    public PageResponse<PickupRequestResponse> getAllRequestsForAdmin(Pageable pageable) {
        return PageResponse.from(pickupRequestRepository.findAll(pageable).map(PickupRequestResponse::from));
    }
}
