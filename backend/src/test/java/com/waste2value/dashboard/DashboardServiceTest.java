package com.waste2value.dashboard;

import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.dashboard.dto.AdminDashboardStats;
import com.waste2value.dashboard.dto.CollectorDashboardStats;
import com.waste2value.dashboard.dto.GeneratorDashboardStats;
import com.waste2value.dashboard.dto.ProcessorDashboardStats;
import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.ProcessorReceipt;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.report.ReportRepository;
import com.waste2value.report.ReportStatus;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

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
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessorReceiptRepository processorReceiptRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Generator Dashboard: accurately aggregates active, draft, completed, cancelled, total quantity, and recent listings")
    void testGeneratorDashboardStats() {
        Long generatorId = 2L;
        when(listingRepository.countByGeneratorIdAndStatus(generatorId, ListingStatus.POSTED)).thenReturn(3L);
        when(listingRepository.countByGeneratorIdAndStatus(generatorId, ListingStatus.DRAFT)).thenReturn(1L);
        when(listingRepository.countByGeneratorIdAndStatus(generatorId, ListingStatus.COMPLETED)).thenReturn(4L);
        when(listingRepository.countByGeneratorIdAndStatus(generatorId, ListingStatus.CANCELLED)).thenReturn(0L);
        when(listingRepository.countByGeneratorId(generatorId)).thenReturn(8L);
        when(listingRepository.sumQuantityByGeneratorId(generatorId)).thenReturn(new BigDecimal("1500.00"));
        when(listingRepository.findTop5ByGeneratorIdOrderByCreatedAtDesc(generatorId)).thenReturn(List.of());

        GeneratorDashboardStats stats = dashboardService.getGeneratorStats(generatorId);

        assertThat(stats).isNotNull();
        assertThat(stats.activeListingsCount()).isEqualTo(3L);
        assertThat(stats.draftListingsCount()).isEqualTo(1L);
        assertThat(stats.completedListingsCount()).isEqualTo(4L);
        assertThat(stats.totalListingsCount()).isEqualTo(8L);
        assertThat(stats.totalQuantityListed()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Collector Dashboard: aggregates verification status, requests, completed pickups, financial volume, and rating")
    void testCollectorDashboardStats() {
        Long collectorId = 3L;
        User collector = new User();
        collector.setId(collectorId);
        collector.setRole(Role.COLLECTOR);

        CollectorProfile profile = new CollectorProfile();
        profile.setId(1L);
        profile.setUser(collector);
        profile.setVerified(true);
        profile.setAvgRating(new BigDecimal("4.85"));

        when(collectorProfileRepository.findByUserId(collectorId)).thenReturn(Optional.of(profile));
        when(listingRepository.countByStatus(ListingStatus.POSTED)).thenReturn(12L);
        when(pickupRequestRepository.countByCollectorIdAndStatus(collectorId, PickupRequestStatus.REQUESTED)).thenReturn(2L);
        when(pickupRequestRepository.countByCollectorIdAndStatus(collectorId, PickupRequestStatus.ACCEPTED)).thenReturn(1L);
        when(pickupRequestRepository.countByCollectorIdAndStatus(collectorId, PickupRequestStatus.COMPLETED)).thenReturn(7L);
        when(transactionRepository.sumQuantityByCollectorId(collectorId)).thenReturn(new BigDecimal("820.00"));
        when(transactionRepository.sumTotalAmountByCollectorId(collectorId)).thenReturn(new BigDecimal("16400.00"));
        when(pickupRequestRepository.findByCollectorId(eq(collectorId), any())).thenReturn(new PageImpl<>(List.of()));

        CollectorDashboardStats stats = dashboardService.getCollectorStats(collectorId);

        assertThat(stats).isNotNull();
        assertThat(stats.isProfileComplete()).isTrue();
        assertThat(stats.isVerified()).isTrue();
        assertThat(stats.availableListingsCount()).isEqualTo(12L);
        assertThat(stats.activeRequestsCount()).isEqualTo(2L);
        assertThat(stats.acceptedPickupsCount()).isEqualTo(1L);
        assertThat(stats.completedPickupsCount()).isEqualTo(7L);
        assertThat(stats.totalQuantityCollected()).isEqualTo(new BigDecimal("820.00"));
        assertThat(stats.totalTransactionValue()).isEqualTo(new BigDecimal("16400.00"));
        assertThat(stats.avgRating()).isEqualTo(new BigDecimal("4.85"));
    }

    @Test
    @DisplayName("Processor Dashboard: aggregates load, capacity, available headroom, capabilities, and receipts")
    void testProcessorDashboardStats() {
        Long processorUserId = 4L;
        User processorUser = new User();
        processorUser.setId(processorUserId);
        processorUser.setRole(Role.PROCESSOR);

        ProcessorProfile profile = new ProcessorProfile();
        profile.setId(10L);
        profile.setUser(processorUser);
        profile.setCompanyName("Green Processing Ltd");
        profile.setVerified(true);
        profile.setMaxCapacityKg(new BigDecimal("10000.00"));
        profile.setCurrentLoadKg(new BigDecimal("6500.00"));
        profile.setAvgRating(new BigDecimal("4.90"));

        when(processorProfileRepository.findByUserId(processorUserId)).thenReturn(Optional.of(profile));
        when(processorCapabilityRepository.findByProcessorId(10L)).thenReturn(List.of(new ProcessorCapability(), new ProcessorCapability()));
        when(processorReceiptRepository.countByProcessorId(10L)).thenReturn(24L);
        when(processorReceiptRepository.findByProcessorId(eq(10L), any())).thenReturn(new PageImpl<>(List.of()));

        ProcessorDashboardStats stats = dashboardService.getProcessorStats(processorUserId);

        assertThat(stats).isNotNull();
        assertThat(stats.isProfileComplete()).isTrue();
        assertThat(stats.isVerified()).isTrue();
        assertThat(stats.maxCapacityKg()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(stats.currentLoadKg()).isEqualTo(new BigDecimal("6500.00"));
        assertThat(stats.availableCapacityKg()).isEqualTo(new BigDecimal("3500.00")); // 10000 - 6500
        assertThat(stats.activeCapabilitiesCount()).isEqualTo(2);
        assertThat(stats.totalReceiptsCount()).isEqualTo(24L);
        assertThat(stats.avgRating()).isEqualTo(new BigDecimal("4.90"));
    }

    @Test
    @DisplayName("Admin Dashboard: aggregates platform-wide stats across users, pending verifications, listings, pickups, transactions, and reports")
    void testAdminDashboardStats() {
        when(collectorProfileRepository.findByIsVerifiedFalse(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 3));
        when(processorProfileRepository.findByIsVerifiedFalse(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 2));
        when(userRepository.count()).thenReturn(50L);
        when(listingRepository.countByStatus(ListingStatus.POSTED)).thenReturn(15L);
        when(pickupRequestRepository.countByStatus(PickupRequestStatus.ACCEPTED)).thenReturn(4L);
        when(transactionRepository.count()).thenReturn(28L);
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(1L);

        AdminDashboardStats stats = dashboardService.getAdminStats();

        assertThat(stats).isNotNull();
        assertThat(stats.pendingCollectorsCount()).isEqualTo(3L);
        assertThat(stats.pendingProcessorsCount()).isEqualTo(2L);
        assertThat(stats.totalUsersCount()).isEqualTo(50L);
        assertThat(stats.activeListingsCount()).isEqualTo(15L);
        assertThat(stats.activePickupRequestsCount()).isEqualTo(4L);
        assertThat(stats.totalTransactionsCount()).isEqualTo(28L);
        assertThat(stats.openReportsCount()).isEqualTo(1L);
    }
}
