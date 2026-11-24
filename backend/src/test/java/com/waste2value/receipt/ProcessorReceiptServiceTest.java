package com.waste2value.receipt;

import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.dto.CreateProcessorReceiptRequest;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.transaction.Transaction;
import com.waste2value.transaction.TransactionRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessorReceiptServiceTest {

    @Mock
    private ProcessorReceiptRepository processorReceiptRepository;

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @Mock
    private ProcessorCapabilityRepository processorCapabilityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProcessorReceiptService processorReceiptService;

    private User processorUser;
    private User collectorUser;
    private User generatorUser;
    private ProcessorProfile verifiedProcessor;
    private Category category;
    private Listing completedListing;
    private PickupRequest completedPickup;
    private Transaction matchingTransaction;

    @BeforeEach
    void setUp() {
        processorUser = new User();
        processorUser.setId(4L);
        processorUser.setRole(Role.PROCESSOR);

        collectorUser = new User();
        collectorUser.setId(3L);
        collectorUser.setRole(Role.COLLECTOR);
        collectorUser.setFirstName("City");
        collectorUser.setLastName("Collector");

        generatorUser = new User();
        generatorUser.setId(2L);
        generatorUser.setRole(Role.GENERATOR);
        generatorUser.setFirstName("Eco");
        generatorUser.setLastName("Generator");

        verifiedProcessor = new ProcessorProfile();
        verifiedProcessor.setId(1L);
        verifiedProcessor.setUser(processorUser);
        verifiedProcessor.setCompanyName("Eco Refining Ltd");
        verifiedProcessor.setVerified(true);
        verifiedProcessor.setMaxCapacityKg(new BigDecimal("5000.00"));
        verifiedProcessor.setCurrentLoadKg(new BigDecimal("4800.00")); // Only 200 kg available

        category = new Category();
        category.setId(8L);
        category.setName("PET Bottles");
        category.setUnit("kg");

        completedListing = new Listing();
        completedListing.setId(100L);
        completedListing.setGenerator(generatorUser);
        completedListing.setCategory(category);
        completedListing.setTitle("PET Bottles Bales");
        completedListing.setQuantity(new BigDecimal("200.00"));
        completedListing.setUnit("kg");

        completedPickup = new PickupRequest();
        completedPickup.setId(501L);
        completedPickup.setListing(completedListing);
        completedPickup.setCollector(collectorUser);
        completedPickup.setStatus(PickupRequestStatus.COMPLETED);
        completedPickup.setActualQuantity(new BigDecimal("200.00"));
        completedPickup.setProposedPrice(new BigDecimal("18.00"));

        matchingTransaction = new Transaction();
        matchingTransaction.setId(1001L);
        matchingTransaction.setListing(completedListing);
        matchingTransaction.setPickupRequest(completedPickup);
        matchingTransaction.setGenerator(generatorUser);
        matchingTransaction.setCollector(collectorUser);
        matchingTransaction.setQuantity(new BigDecimal("200.00"));
        matchingTransaction.setUnit("kg");
        matchingTransaction.setAgreedPrice(new BigDecimal("18.00"));
        matchingTransaction.setTotalAmount(new BigDecimal("3600.00"));
    }

    @Test
    @DisplayName("Receipt exceeding available facility capacity is rejected and capacity is not altered")
    void receiptExceedingCapacityRejected() {
        when(processorProfileRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(verifiedProcessor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(processorCapabilityRepository.findByProcessorIdAndCategoryId(1L, 8L)).thenReturn(Optional.empty());

        // Attempting to deliver 300 kg when only 200 kg available
        CreateProcessorReceiptRequest req = new CreateProcessorReceiptRequest(
                3L, 8L, null, null, new BigDecimal("300.00"), "kg", new BigDecimal("22.00"), "Direct truck drop-off"
        );

        assertThatThrownBy(() -> processorReceiptService.recordReceipt(4L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Exceeds facility capacity");

        // Verify processor profile load remained unchanged
        assertThat(verifiedProcessor.getCurrentLoadKg()).isEqualTo(new BigDecimal("4800.00"));
        verify(processorProfileRepository, never()).save(any());
        verify(processorReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Valid direct receipt updates processor currentLoadKg and saves receipt")
    void validReceiptUpdatesLoadAndSaves() {
        when(processorProfileRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(verifiedProcessor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(processorCapabilityRepository.findByProcessorIdAndCategoryId(1L, 8L)).thenReturn(Optional.empty());
        when(processorReceiptRepository.save(any(ProcessorReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Deliver 150 kg (which fits within the 200 kg available)
        CreateProcessorReceiptRequest req = new CreateProcessorReceiptRequest(
                3L, 8L, null, null, new BigDecimal("150.00"), "kg", new BigDecimal("22.00"), "Clean bales"
        );

        ProcessorReceiptResponse resp = processorReceiptService.recordReceipt(4L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.quantity()).isEqualTo(new BigDecimal("150.00"));
        assertThat(verifiedProcessor.getCurrentLoadKg()).isEqualTo(new BigDecimal("4950.00")); // 4800 + 150
        verify(processorProfileRepository, times(1)).save(verifiedProcessor);
        verify(notificationService, times(1)).sendNotification(eq(collectorUser), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Traceability: Linked pickup request validates status, matches collector and category, and links transaction")
    void receiptLinkedToPickupRequestTracesFullLifecycle() {
        when(processorProfileRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(verifiedProcessor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(completedPickup));
        when(transactionRepository.findByPickupRequestId(501L)).thenReturn(Optional.of(matchingTransaction));
        when(processorCapabilityRepository.findByProcessorIdAndCategoryId(1L, 8L)).thenReturn(Optional.empty());
        when(processorReceiptRepository.save(any(ProcessorReceipt.class))).thenAnswer(i -> i.getArgument(0));

        CreateProcessorReceiptRequest req = new CreateProcessorReceiptRequest(
                3L, 8L, 100L, 501L, new BigDecimal("100.00"), "kg", new BigDecimal("20.00"), "Verified batch intake"
        );

        ProcessorReceiptResponse resp = processorReceiptService.recordReceipt(4L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.listingId()).isEqualTo(100L);
        assertThat(resp.pickupRequestId()).isEqualTo(501L);
        assertThat(resp.transactionId()).isEqualTo(1001L);
        assertThat(resp.generatorId()).isEqualTo(2L);
        assertThat(resp.generatorName()).isEqualTo("Eco Generator");
        assertThat(resp.totalAmount()).isEqualTo(new BigDecimal("2000.00")); // 100 * 20.00

        // Verify transaction was linked to this processor
        assertThat(matchingTransaction.getProcessor()).isEqualTo(verifiedProcessor);
        verify(transactionRepository, times(1)).save(matchingTransaction);
    }

    @Test
    @DisplayName("Traceability Guard: Non-COMPLETED pickup request is rejected")
    void nonCompletedPickupRequestRejected() {
        completedPickup.setStatus(PickupRequestStatus.ACCEPTED); // Not completed yet

        when(processorProfileRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(verifiedProcessor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(completedPickup));

        CreateProcessorReceiptRequest req = new CreateProcessorReceiptRequest(
                3L, 8L, 100L, 501L, new BigDecimal("100.00"), "kg", new BigDecimal("20.00"), "Premature receipt"
        );

        assertThatThrownBy(() -> processorReceiptService.recordReceipt(4L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only be linked to a COMPLETED pickup request");
    }

    @Test
    @DisplayName("Traceability Guard: Mismatched collector on pickup request is rejected")
    void mismatchedCollectorRejected() {
        User anotherCollector = new User();
        anotherCollector.setId(99L);
        anotherCollector.setRole(Role.COLLECTOR);
        completedPickup.setCollector(anotherCollector);

        when(processorProfileRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(verifiedProcessor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(pickupRequestRepository.findById(501L)).thenReturn(Optional.of(completedPickup));

        CreateProcessorReceiptRequest req = new CreateProcessorReceiptRequest(
                3L, 8L, 100L, 501L, new BigDecimal("100.00"), "kg", new BigDecimal("20.00"), "Collector mismatch"
        );

        assertThatThrownBy(() -> processorReceiptService.recordReceipt(4L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match the collector who fulfilled pickup request");
    }

    @Test
    @DisplayName("Authorization Guard: Unrelated user cannot view processor receipt by id")
    void unauthorizedUserCannotViewReceipt() {
        ProcessorReceipt receipt = new ProcessorReceipt();
        receipt.setId(77L);
        receipt.setProcessor(verifiedProcessor);
        receipt.setCollector(collectorUser);
        receipt.setCategory(category);
        receipt.setListing(completedListing);

        when(processorReceiptRepository.findById(77L)).thenReturn(Optional.of(receipt));

        // User id 99 (unrelated) attempts to view
        assertThatThrownBy(() -> processorReceiptService.getReceiptById(99L, Role.COLLECTOR, 77L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized to view this processor receipt");

        // Verified processor owner (id 4) CAN view
        ProcessorReceiptResponse resp = processorReceiptService.getReceiptById(4L, Role.PROCESSOR, 77L);
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isEqualTo(77L);
    }
}
