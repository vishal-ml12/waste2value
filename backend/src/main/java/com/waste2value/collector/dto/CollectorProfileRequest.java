package com.waste2value.collector.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CollectorProfileRequest(
        @NotBlank(message = "Vehicle type is required")
        @Size(max = 100, message = "Vehicle type must not exceed 100 characters")
        String vehicleType,

        @NotBlank(message = "Service area is required")
        @Size(max = 255, message = "Service area must not exceed 255 characters")
        String serviceArea,

        @NotNull(message = "Max distance is required")
        @DecimalMin(value = "1.0", message = "Max distance must be at least 1 km")
        BigDecimal maxDistanceKm
) {}
