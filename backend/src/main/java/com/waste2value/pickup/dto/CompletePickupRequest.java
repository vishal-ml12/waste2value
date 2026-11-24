package com.waste2value.pickup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompletePickupRequest(
        @NotNull(message = "Actual quantity is required")
        @DecimalMin(value = "0.01", message = "Actual quantity must be greater than zero")
        BigDecimal actualQuantity,

        @Size(max = 1000, message = "Completion note cannot exceed 1000 characters")
        String completionNote
) {}
