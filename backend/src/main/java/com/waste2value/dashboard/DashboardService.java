package com.waste2value.dashboard;

import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.dashboard.dto.AdminDashboardStats;
import com.waste2value.dashboard.dto.CollectorDashboardStats;
import com.waste2value.dashboard.dto.GeneratorDashboardStats;
import com.waste2value.dashboard.dto.ProcessorDashboardStats;
import com.waste2value.listing.ListingRepository;
import com.waste2value.listing.ListingStatus;
import com.waste2value.listing.dto.ListingSummaryResponse;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.pickup.dto.PickupRequestResponse;
import com.waste2value.processor.ProcessorCapabilityRepository;
import com.waste2value.processor.ProcessorProfile;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.receipt.ProcessorReceiptRepository;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import com.waste2value.report.ReportRepository;
import com.waste2value.report.ReportStatus;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final ListingRepository listingRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final ProcessorProfileRepository processorProfileRepository;
    private final ProcessorCapabilityRepository processorCapabilityRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final TransactionRepository transactionRepository;
    private final ProcessorReceiptRepository processorReceiptRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public DashboardService(
            ListingRepository listingRepository,
            CollectorProfileRepository collectorProfileRepository,
            ProcessorProfileRepository processorProfileRepository,
            ProcessorCapabilityRepository processorCapabilityRepository,
            PickupRequestRepository pickupRequestRepository,
            TransactionRepository transactionRepository,
            ProcessorReceiptRepository processorReceiptRepository,
            ReportRepository reportRepository,
            UserRepository userRepository
    ) {
        this.listingRepository = listingRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.processorProfileRepository = processorProfileRepository;
        this.processorCapabilityRepository = processorCapabilityRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.transactionRepository = transactionRepository;
        this.processorReceiptRepository = processorReceiptRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GeneratorDashboardStats getGeneratorStats(Long userId) {
        long activeCount = listingRepository.countByGeneratorIdAndStatus(userId, ListingStatus.POSTED);
        long draftCount = listingRepository.countByGeneratorIdAndStatus(userId, ListingStatus.DRAFT);
        long completedCount = listingRepository.countByGeneratorIdAndStatus(userId, ListingStatus.COMPLETED);
        long cancelledCount = listingRepository.countByGeneratorIdAndStatus(userId, ListingStatus.CANCELLED);
        long totalCount = listingRepository.countByGeneratorId(userId);
        BigDecimal totalQuantity = listingRepository.sumQuantityByGeneratorId(userId);

        List<ListingSummaryResponse> recentListings = listingRepository
                .findTop5ByGeneratorIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ListingSummaryResponse::from)
                .toList();

        return new GeneratorDashboardStats(
                activeCount,
                draftCount,
                completedCount,
                cancelledCount,
                totalCount,
                totalQuantity != null ? totalQuantity : BigDecimal.ZERO,
                recentListings
        );
    }

    @Transactional(readOnly = true)
    public CollectorDashboardStats getCollectorStats(Long userId) {
        Optional<CollectorProfile> profileOpt = collectorProfileRepository.findByUserId(userId);
        boolean isComplete = profileOpt.isPresent();
        boolean isVerified = profileOpt.map(CollectorProfile::isVerified).orElse(false);
        BigDecimal avgRating = profileOpt.map(CollectorProfile::getAvgRating).orElse(BigDecimal.ZERO);

        long availableListings = listingRepository.countByStatus(ListingStatus.POSTED);
        long activeRequests = pickupRequestRepository.countByCollectorIdAndStatus(userId, PickupRequestStatus.REQUESTED);
        long acceptedPickups = pickupRequestRepository.countByCollectorIdAndStatus(userId, PickupRequestStatus.ACCEPTED);
        long completedPickups = pickupRequestRepository.countByCollectorIdAndStatus(userId, PickupRequestStatus.COMPLETED);

        BigDecimal totalQuantity = transactionRepository.sumQuantityByCollectorId(userId);
        BigDecimal totalAmount = transactionRepository.sumTotalAmountByCollectorId(userId);

        List<PickupRequestResponse> recentRequests = pickupRequestRepository
                .findByCollectorId(userId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(PickupRequestResponse::from)
                .toList();

        return new CollectorDashboardStats(
                isComplete,
                isVerified,
                availableListings,
                activeRequests,
                acceptedPickups,
                completedPickups,
                totalQuantity,
                totalAmount,
                avgRating,
                recentRequests
        );
    }

    @Transactional(readOnly = true)
    public ProcessorDashboardStats getProcessorStats(Long userId) {
        Optional<ProcessorProfile> profileOpt = processorProfileRepository.findByUserId(userId);
        boolean isComplete = profileOpt.isPresent();
        boolean isVerified = profileOpt.map(ProcessorProfile::isVerified).orElse(false);
        BigDecimal maxCapacity = profileOpt.map(ProcessorProfile::getMaxCapacityKg).orElse(BigDecimal.ZERO);
        BigDecimal currentLoad = profileOpt.map(ProcessorProfile::getCurrentLoadKg).orElse(BigDecimal.ZERO);
        BigDecimal available = maxCapacity.subtract(currentLoad);
        if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;
        BigDecimal avgRating = profileOpt.map(ProcessorProfile::getAvgRating).orElse(BigDecimal.ZERO);

        int capabilitiesCount = profileOpt.map(p -> processorCapabilityRepository.findByProcessorId(p.getId()).size()).orElse(0);
        long totalReceipts = profileOpt.map(p -> processorReceiptRepository.countByProcessorId(p.getId())).orElse(0L);

        List<ProcessorReceiptResponse> recentReceipts = profileOpt.map(p ->
                processorReceiptRepository.findByProcessorId(p.getId(), PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "receivedDate")))
                        .getContent()
                        .stream()
                        .map(ProcessorReceiptResponse::from)
                        .toList()
        ).orElse(List.of());

        return new ProcessorDashboardStats(
                isComplete,
                isVerified,
                maxCapacity,
                currentLoad,
                available,
                capabilitiesCount,
                totalReceipts,
                avgRating,
                recentReceipts
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminStats() {
        long pendingCollectors = collectorProfileRepository.findByIsVerifiedFalse(PageRequest.of(0, 1)).getTotalElements();
        long pendingProcessors = processorProfileRepository.findByIsVerifiedFalse(PageRequest.of(0, 1)).getTotalElements();
        long totalUsers = userRepository.count();
        long activeListings = listingRepository.countByStatus(ListingStatus.POSTED);
        long activePickups = pickupRequestRepository.countByStatus(PickupRequestStatus.ACCEPTED);
        long totalTransactions = transactionRepository.count();
        long openReports = reportRepository.countByStatus(ReportStatus.OPEN);

        return new AdminDashboardStats(
                pendingCollectors,
                pendingProcessors,
                totalUsers,
                activeListings,
                activePickups,
                totalTransactions,
                openReports
        );
    }
}
