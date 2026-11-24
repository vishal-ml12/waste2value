package com.waste2value.report.dto;

import com.waste2value.report.Report;
import com.waste2value.report.ReportStatus;
import com.waste2value.report.ReportTargetType;
import java.time.Instant;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterName,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        String description,
        ReportStatus status,
        String adminNote,
        Long resolvedByUserId,
        String resolvedByUserName,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportResponse from(Report r) {
        String repName = r.getReporter().getFirstName() + " " + r.getReporter().getLastName();
        Long resById = r.getResolvedBy() != null ? r.getResolvedBy().getId() : null;
        String resByName = r.getResolvedBy() != null ? r.getResolvedBy().getFirstName() + " " + r.getResolvedBy().getLastName() : null;

        return new ReportResponse(
                r.getId(),
                r.getReporter().getId(),
                repName,
                r.getTargetType(),
                r.getTargetId(),
                r.getReason(),
                r.getDescription(),
                r.getStatus(),
                r.getAdminNote(),
                resById,
                resByName,
                r.getResolvedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
