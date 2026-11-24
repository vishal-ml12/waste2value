package com.waste2value.receipt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProcessorReceiptRequest(
        @NotNull(message = "Collector ID is required")
        Long collectorId,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        Long listingId,
        Long pickupRequestId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        String unit,

        @NotNull(message = "Agreed price is required")
        @DecimalMin(value = "0.0", message = "Agreed price cannot be negative")
        BigDecimal agreedPrice,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
) {}
