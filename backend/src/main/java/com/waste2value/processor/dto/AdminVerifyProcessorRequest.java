package com.waste2value.processor.dto;

import jakarta.validation.constraints.NotNull;

public record AdminVerifyProcessorRequest(
        @NotNull(message = "Verification status is required")
        Boolean verified,
        String note
) {}
