package com.waste2value.report.dto;

import com.waste2value.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull(message = "Target type is required (LISTING, PICKUP, TRANSACTION, USER)")
        ReportTargetType targetType,

        @NotNull(message = "Target ID is required")
        Long targetId,

        @NotBlank(message = "Reason is required")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {}
