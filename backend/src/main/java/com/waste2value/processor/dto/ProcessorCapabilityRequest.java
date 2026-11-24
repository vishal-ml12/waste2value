package com.waste2value.processor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProcessorCapabilityRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Minimum quantity is required")
        @DecimalMin(value = "0.0", message = "Minimum quantity cannot be negative")
        BigDecimal minQuantity,

        @NotNull(message = "Maximum quantity is required")
        @DecimalMin(value = "0.0", message = "Maximum quantity cannot be negative")
        BigDecimal maxQuantity,

        @NotNull(message = "Price per unit is required")
        @DecimalMin(value = "0.0", message = "Price per unit cannot be negative")
        BigDecimal pricePerUnit,

        Boolean isActive
) {}
