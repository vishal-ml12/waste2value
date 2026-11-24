package com.waste2value.pickup;

import com.waste2value.address.Address;
import com.waste2value.category.Category;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.notification.NotificationService;
import com.waste2value.pickup.dto.CompletePickupRequest;
import com.waste2value.pickup.dto.CreatePickupRequest;
import com.waste2value.pickup.dto.PickupRequestResponse;
import com.waste2value.pickup.dto.RespondPickupRequest;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.transaction.TransactionService;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupRequestServiceTest {

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PickupRequestService pickupRequestService;

    private User generator;
    private User collector;
    private CollectorProfile verifiedProfile;
    private CollectorProfile unverifiedProfile;
    private Listing postedListing;
    private Address address;
    private Category category;

    @BeforeEach
    void setUp() {
        generator = new User();
        generator.setId(2L);
        generator.setRole(Role.GENERATOR);
        generator.setFirstName("Eco");
        generator.setLastName("Generator");

        collector = new User();
        collector.setId(3L);
        collector.setRole(Role.COLLECTOR);
        collector.setFirstName("City");
        collector.setLastName("Collector");

        verifiedProfile = new CollectorProfile();
        verifiedProfile.setId(1L);
        verifiedProfile.setUser(collector);
        verifiedProfile.setVerified(true);

        unverifiedProfile = new CollectorProfile();
        unverifiedProfile.setId(2L);
        unverifiedProfile.setUser(collector);
        unverifiedProfile.setVerified(false);

        address = new Address();
        address.setId(1L);
        address.setUser(generator);
        address.setAddressLine1("Plot 42, Tech Park");
        address.setCity("Mumbai");
        address.setState("Maharashtra");

        category = new Category();
        category.setId(8L);
        category.setName("PET Bottles");
        category.setUnit("kg");

        postedListing = new Listing();
        postedListing.setId(100L);
        postedListing.setGenerator(generator);
        postedListing.setCategory(category);
        postedListing.setAddress(address);
        postedListing.setTitle("PET Bottles Listing");
        postedListing.setQuantity(new BigDecimal("200.00"));
        postedListing.setUnit("kg");
        postedListing.setExpectedPrice(new BigDecimal("18.00"));
        postedListing.setStatus(ListingStatus.POSTED);
    }

    @Test
    @DisplayName("Unverified collector cannot create pickup request")
    void unverifiedCollectorCannotCreateRequest() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(collector));
        when(collectorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(unverifiedProfile));

        CreatePickupRequest req = new CreatePickupRequest(new BigDecimal("18.00"), "Can pickup tomorrow");

        assertThatThrownBy(() -> pickupRequestService.createPickupRequest(3L, 100L, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("pending admin verification");
    }

    @Test
    @DisplayName("Generator cannot request pickup on own listing")
    void generatorCannotRequestOwnListing() {
        User generatorCollector = new User();
        generatorCollector.setId(2L);
        generatorCollector.setRole(Role.COLLECTOR);

        when(userRepository.findById(2L)).thenReturn(Optional.of(generatorCollector));
        when(collectorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(verifiedProfile));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(postedListing));

        CreatePickupRequest req = new CreatePickupRequest(new BigDecimal("18.00"), "Self request");

        assertThatThrownBy(() -> pickupRequestService.createPickupRequest(2L, 100L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot request pickup for their own listing");
    }

    @Test
    @DisplayName("Safe allocation: Accepting request marks it ACCEPTED, allocates listing, and rejects competing requests")
    void safeAllocationRejectsCompetingRequests() {
        PickupRequest winningRequest = new PickupRequest();
        winningRequest.setId(501L);
        winningRequest.setListing(postedListing);
        winningRequest.setCollector(collector);
        winningRequest.setStatus(PickupRequestStatus.REQUESTED);
        winningRequest.setProposedPrice(new BigDecimal("18.00"));

        User collector2 = new User();
        collector2.setId(5L);
        collector2.setRole(Role.COLLECTOR);
        collector2.setFirstName("Other");
        collector2.setLastName("Collector");

        PickupRequest competingRequest = new PickupRequest();
        competingRequest.setId(502L);
        competingRequest.setListing(postedListing);
        competingRequest.setCollector(collector2);
        competingRequest.setStatus(PickupRequestStatus.REQUESTED);
        competingRequest.setProposedPrice(new BigDecimal("17.00"));

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(winningRequest));
        when(listingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(postedListing));
        when(pickupRequestRepository.findByListingIdAndStatus(100L, PickupRequestStatus.REQUESTED))
                .thenReturn(List.of(winningRequest, competingRequest));
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> i.getArgument(0));

        RespondPickupRequest respond = new RespondPickupRequest(true, "Deal agreed");
        PickupRequestResponse response = pickupRequestService.respondToRequest(2L, 501L, respond);

        assertThat(response.status()).isEqualTo(PickupRequestStatus.ACCEPTED);
        assertThat(postedListing.getStatus()).isEqualTo(ListingStatus.ALLOCATED);
        assertThat(competingRequest.getStatus()).isEqualTo(PickupRequestStatus.REJECTED);

        verify(notificationService, atLeastOnce()).sendNotification(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Accepting request on already ALLOCATED listing throws ConflictException")
    void acceptingAlreadyAllocatedListingThrowsConflict() {
        postedListing.setStatus(ListingStatus.ALLOCATED);

        PickupRequest req = new PickupRequest();
        req.setId(501L);
        req.setListing(postedListing);
        req.setCollector(collector);
        req.setStatus(PickupRequestStatus.REQUESTED);

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(req));
        when(listingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(postedListing));

        RespondPickupRequest respond = new RespondPickupRequest(true, "Accept");

        assertThatThrownBy(() -> pickupRequestService.respondToRequest(2L, 501L, respond))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been allocated");
    }

    @Test
    @DisplayName("Completing pickup transitions request and listing to COMPLETED and generates Transaction")
    void completePickupTransitionsAndGeneratesTransaction() {
        postedListing.setStatus(ListingStatus.ALLOCATED);

        PickupRequest acceptedRequest = new PickupRequest();
        acceptedRequest.setId(501L);
        acceptedRequest.setListing(postedListing);
        acceptedRequest.setCollector(collector);
        acceptedRequest.setStatus(PickupRequestStatus.ACCEPTED);
        acceptedRequest.setProposedPrice(new BigDecimal("18.00"));

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(acceptedRequest));
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(collectorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(verifiedProfile));

        CompletePickupRequest completeReq = new CompletePickupRequest(new BigDecimal("210.00"), "Collected cleanly");
        PickupRequestResponse response = pickupRequestService.completePickup(3L, 501L, completeReq);

        assertThat(response.status()).isEqualTo(PickupRequestStatus.COMPLETED);
        assertThat(postedListing.getStatus()).isEqualTo(ListingStatus.COMPLETED);
        assertThat(response.actualQuantity()).isEqualTo(new BigDecimal("210.00"));

        verify(transactionService, times(1)).createTransactionForPickup(eq(acceptedRequest), eq(new BigDecimal("210.00")));
    }

    @Test
    @DisplayName("Lifecycle Consistency: Collector cancelling accepted request reverts listing status to POSTED")
    void collectorCancelsAcceptedRequestRevertsListingToPosted() {
        postedListing.setStatus(ListingStatus.ALLOCATED);

        PickupRequest acceptedRequest = new PickupRequest();
        acceptedRequest.setId(501L);
        acceptedRequest.setListing(postedListing);
        acceptedRequest.setCollector(collector);
        acceptedRequest.setStatus(PickupRequestStatus.ACCEPTED);

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(acceptedRequest));
        when(listingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(postedListing));
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> i.getArgument(0));

        PickupRequestResponse resp = pickupRequestService.cancelRequest(3L, Role.COLLECTOR, 501L);

        assertThat(resp.status()).isEqualTo(PickupRequestStatus.CANCELLED);
        assertThat(postedListing.getStatus()).isEqualTo(ListingStatus.POSTED);
        verify(listingRepository, times(1)).save(postedListing);
        verify(notificationService, times(1)).sendNotification(eq(generator), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Lifecycle Consistency: Generator cancelling accepted request reverts listing status to POSTED")
    void generatorCancelsAcceptedRequestRevertsListingToPosted() {
        postedListing.setStatus(ListingStatus.ALLOCATED);

        PickupRequest acceptedRequest = new PickupRequest();
        acceptedRequest.setId(501L);
        acceptedRequest.setListing(postedListing);
        acceptedRequest.setCollector(collector);
        acceptedRequest.setStatus(PickupRequestStatus.ACCEPTED);

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(acceptedRequest));
        when(listingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(postedListing));
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> i.getArgument(0));

        PickupRequestResponse resp = pickupRequestService.cancelRequest(2L, Role.GENERATOR, 501L);

        assertThat(resp.status()).isEqualTo(PickupRequestStatus.CANCELLED);
        assertThat(postedListing.getStatus()).isEqualTo(ListingStatus.POSTED);
        verify(listingRepository, times(1)).save(postedListing);
        verify(notificationService, times(1)).sendNotification(eq(collector), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Authorization Guard: Unrelated user cannot cancel pickup request")
    void unauthorizedUserCannotCancelRequest() {
        PickupRequest req = new PickupRequest();
        req.setId(501L);
        req.setListing(postedListing);
        req.setCollector(collector);
        req.setStatus(PickupRequestStatus.REQUESTED);

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> pickupRequestService.cancelRequest(99L, Role.COLLECTOR, 501L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only cancel pickup requests you participate in");
    }

    @Test
    @DisplayName("Authorization Guard: Non-owner generator cannot accept or reject pickup requests")
    void nonOwnerGeneratorCannotRespondToPickupRequest() {
        PickupRequest req = new PickupRequest();
        req.setId(501L);
        req.setListing(postedListing); // Owned by generator (id 2)
        req.setCollector(collector);
        req.setStatus(PickupRequestStatus.REQUESTED);

        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(req));

        RespondPickupRequest respond = new RespondPickupRequest(true, "Fraudulent accept");

        // User id 99 (another generator) attempts to respond
        assertThatThrownBy(() -> pickupRequestService.respondToRequest(99L, 501L, respond))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the waste generator who created this listing can respond");
    }
}
