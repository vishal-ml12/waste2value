package com.waste2value.collector.dto;

import jakarta.validation.constraints.NotNull;

public record AdminVerifyCollectorRequest(
        @NotNull(message = "Verification status is required")
        Boolean verified,
        String note
) {}
