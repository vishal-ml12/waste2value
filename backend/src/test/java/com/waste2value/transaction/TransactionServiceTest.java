package com.waste2value.transaction;

import com.waste2value.category.Category;
import com.waste2value.listing.Listing;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.transaction.dto.TransactionResponse;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User generator;
    private User collector;
    private User processorUser;
    private ProcessorProfile processorProfile;
    private Listing listing;
    private PickupRequest pickupRequest;
    private Transaction transaction;

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

        processorUser = new User();
        processorUser.setId(4L);
        processorUser.setRole(Role.PROCESSOR);

        processorProfile = new ProcessorProfile();
        processorProfile.setId(10L);
        processorProfile.setUser(processorUser);
        processorProfile.setCompanyName("Eco Polymer Plant");

        Category category = new Category();
        category.setId(1L);
        category.setName("Plastics");
        category.setUnit("kg");

        listing = new Listing();
        listing.setId(100L);
        listing.setGenerator(generator);
        listing.setCategory(category);
        listing.setUnit("kg");

        pickupRequest = new PickupRequest();
        pickupRequest.setId(500L);
        pickupRequest.setListing(listing);
        pickupRequest.setCollector(collector);
        pickupRequest.setProposedPrice(new BigDecimal("15.50"));

        transaction = new Transaction();
        transaction.setId(900L);
        transaction.setListing(listing);
        transaction.setPickupRequest(pickupRequest);
        transaction.setGenerator(generator);
        transaction.setCollector(collector);
        transaction.setProcessor(processorProfile);
        transaction.setQuantity(new BigDecimal("100.00"));
        transaction.setUnit("kg");
        transaction.setAgreedPrice(new BigDecimal("15.50"));
        transaction.setTotalAmount(new BigDecimal("1550.00"));
        transaction.setStatus(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Create transaction calculates BigDecimal financial total and saves record")
    void createTransactionCalculatesFinancialTotal() {
        when(transactionRepository.findByPickupRequestId(500L)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.createTransactionForPickup(pickupRequest, new BigDecimal("120.50"));

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(new BigDecimal("120.50"));
        assertThat(result.getAgreedPrice()).isEqualTo(new BigDecimal("15.50"));
        // 120.50 * 15.50 = 1867.75
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("1867.75"));
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Duplicate transaction creation for same pickup request throws ConflictException")
    void duplicateTransactionThrowsConflict() {
        when(transactionRepository.findByPickupRequestId(500L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.createTransactionForPickup(pickupRequest, new BigDecimal("100.00")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Transaction record already exists");
    }

    @Test
    @DisplayName("Get my transactions queries processor receipts when role is PROCESSOR")
    void getMyTransactionsForProcessorQueriesProcessorId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(processorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(processorProfile));
        when(transactionRepository.findByProcessorId(10L, pageable)).thenReturn(new PageImpl<>(List.of(transaction)));

        PageResponse<TransactionResponse> response = transactionService.getMyTransactions(4L, Role.PROCESSOR, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Transaction access authorization: Participants can view, unrelated user is rejected with ForbiddenException")
    void transactionAccessAuthorization() {
        when(transactionRepository.findById(900L)).thenReturn(Optional.of(transaction));

        // Generator (id 2) can view
        assertThat(transactionService.getTransactionById(2L, Role.GENERATOR, 900L)).isNotNull();

        // Collector (id 3) can view
        assertThat(transactionService.getTransactionById(3L, Role.COLLECTOR, 900L)).isNotNull();

        // Processor (id 4) can view
        assertThat(transactionService.getTransactionById(4L, Role.PROCESSOR, 900L)).isNotNull();

        // Admin (id 1) can view
        assertThat(transactionService.getTransactionById(1L, Role.ADMIN, 900L)).isNotNull();

        // Unrelated user (id 99) is denied
        assertThatThrownBy(() -> transactionService.getTransactionById(99L, Role.COLLECTOR, 900L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to view this transaction");
    }
}
