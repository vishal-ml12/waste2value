package com.waste2value.report.dto;

import com.waste2value.report.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReportStatusRequest(
        @NotNull(message = "Report status is required (OPEN, UNDER_REVIEW, RESOLVED, DISMISSED)")
        ReportStatus status,

        @Size(max = 2000, message = "Admin note must not exceed 2000 characters")
        String adminNote
) {}
