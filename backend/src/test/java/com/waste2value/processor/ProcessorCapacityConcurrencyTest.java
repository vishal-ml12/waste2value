package com.waste2value.processor;

import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.receipt.ProcessorReceiptService;
import com.waste2value.receipt.dto.CreateProcessorReceiptRequest;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProcessorCapacityConcurrencyTest {

    @Autowired
    private ProcessorReceiptService processorReceiptService;

    @Autowired
    private ProcessorProfileRepository processorProfileRepository;

    @Autowired
    private ProcessorReceiptRepository processorReceiptRepository;

    @Autowired
    private com.waste2value.notification.NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User collector;
    private User processorUser;
    private ProcessorProfile processorProfile;
    private Category category;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        collector = new User();
        collector.setFirstName("Dan");
        collector.setLastName("Collector");
        collector.setEmail("collector-" + uid + "@test.com");
        collector.setPasswordHash("hashedpass");
        collector.setPhone("9876543210");
        collector.setRole(Role.COLLECTOR);
        collector.setStatus(AccountStatus.ACTIVE);
        collector = userRepository.save(collector);

        processorUser = new User();
        processorUser.setFirstName("Poly");
        processorUser.setLastName("Processor");
        processorUser.setEmail("processor-" + uid + "@test.com");
        processorUser.setPasswordHash("hashedpass");
        processorUser.setPhone("9876543211");
        processorUser.setRole(Role.PROCESSOR);
        processorUser.setStatus(AccountStatus.ACTIVE);
        processorUser = userRepository.save(processorUser);

        processorProfile = new ProcessorProfile();
        processorProfile.setUser(processorUser);
        processorProfile.setCompanyName("Concurrency Test Facility " + uid);
        processorProfile.setLicenseNumber("LIC-" + uid);
        processorProfile.setMaxCapacityKg(new BigDecimal("1000.00"));
        processorProfile.setCurrentLoadKg(new BigDecimal("900.00")); // Only 100 kg capacity available!
        processorProfile.setVerified(true);
        processorProfile = processorProfileRepository.save(processorProfile);

        category = new Category();
        category.setName("Category-" + uid);
        category.setSlug("cat-" + uid);
        category.setUnit("kg");
        category = categoryRepository.save(category);
    }

    @AfterEach
    void tearDown() {
        processorReceiptRepository.deleteAll();
        notificationRepository.deleteAll();
        processorProfileRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Audit 2 Concurrency: Two simultaneous receipts competing for remaining capacity - only one succeeds, no over-capacity")
    void concurrentIntakeRequestsCannotExceedCapacity() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        // Both requests ask for 80 kg, while only 100 kg is available (900/1000).
        // If concurrency is properly serialized with pessimistic locking,
        // exactly ONE can succeed (900 + 80 = 980), and the second must be rejected (980 + 80 = 1060 > 1000).
        CreateProcessorReceiptRequest requestA = new CreateProcessorReceiptRequest(
                collector.getId(), category.getId(), null, null, new BigDecimal("80.00"), "kg", new BigDecimal("12.00"), "Thread A drop"
        );
        CreateProcessorReceiptRequest requestB = new CreateProcessorReceiptRequest(
                collector.getId(), category.getId(), null, null, new BigDecimal("80.00"), "kg", new BigDecimal("12.00"), "Thread B drop"
        );

        List<CreateProcessorReceiptRequest> requests = List.of(requestA, requestB);

        for (int i = 0; i < threads; i++) {
            final CreateProcessorReceiptRequest req = requests.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ProcessorReceiptResponse resp = processorReceiptService.recordReceipt(processorUser.getId(), req);
                    if (resp != null) {
                        successCount.incrementAndGet();
                    }
                } catch (BadRequestException e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
        }

        // Wait until both threads are primed and ready
        readyLatch.await(5, TimeUnit.SECONDS);
        // Release both threads simultaneously
        startLatch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Verification: Exactly one request succeeded and exactly one failed with capacity rejection
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(exceptions.get(0).getMessage()).contains("Exceeds facility capacity");

        // Verification: Database state is completely consistent and did not exceed max capacity
        ProcessorProfile updatedProfile = processorProfileRepository.findById(processorProfile.getId()).orElseThrow();
        assertThat(updatedProfile.getCurrentLoadKg()).isEqualTo(new BigDecimal("980.00")); // Exactly 900 + 80

        // Verification: Exactly 1 receipt record exists in database
        assertThat(processorReceiptRepository.findByProcessorId(processorProfile.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .isEqualTo(1);
    }
}
