package com.waste2value.processor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProcessorProfileRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must not exceed 150 characters")
        String companyName,

        @NotBlank(message = "License number is required")
        @Size(max = 100, message = "License number must not exceed 100 characters")
        String licenseNumber,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Maximum capacity is required")
        @DecimalMin(value = "0.0", message = "Maximum capacity cannot be negative")
        BigDecimal maxCapacityKg
) {}
