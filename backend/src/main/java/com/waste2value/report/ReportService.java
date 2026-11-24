package com.waste2value.report;

import com.waste2value.listing.ListingRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.report.dto.CreateReportRequest;
import com.waste2value.report.dto.ReportResponse;
import com.waste2value.report.dto.UpdateReportStatusRequest;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.transaction.TransactionRepository;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ListingRepository listingRepository,
            PickupRequestRepository pickupRequestRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportResponse createReport(Long reporterUserId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reporterUserId));

        // Validate target existence
        boolean exists = switch (request.targetType()) {
            case LISTING -> listingRepository.existsById(request.targetId());
            case PICKUP -> pickupRequestRepository.existsById(request.targetId());
            case TRANSACTION -> transactionRepository.existsById(request.targetId());
            case USER -> userRepository.existsById(request.targetId());
        };

        if (!exists) {
            throw new ResourceNotFoundException("Reported target " + request.targetType() + " not found with id: " + request.targetId());
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDescription(request.description());
        report.setStatus(ReportStatus.OPEN);

        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getMyReports(Long reporterUserId, Pageable pageable) {
        return PageResponse.from(
                reportRepository.findByReporterId(reporterUserId, pageable).map(ReportResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return PageResponse.from(reportRepository.findByStatus(status, pageable).map(ReportResponse::from));
        }
        return PageResponse.from(reportRepository.findAll(pageable).map(ReportResponse::from));
    }

    @Transactional
    public ReportResponse updateReportStatus(Long adminUserId, Long reportId, UpdateReportStatusRequest request) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        report.setStatus(request.status());
        report.setAdminNote(request.adminNote());

        if (request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.DISMISSED) {
            report.setResolvedBy(admin);
            report.setResolvedAt(Instant.now());

            // Notify reporter
            notificationService.sendNotification(
                    report.getReporter(),
                    NotificationType.REPORT_UPDATE,
                    "Dispute Report Status: " + request.status(),
                    "Your report on " + report.getTargetType() + " #" + report.getTargetId() + " has been " + request.status() + ". Note: " + (request.adminNote() != null ? request.adminNote() : "None"),
                    "REPORT",
                    report.getId()
            );
        }

        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }
}
