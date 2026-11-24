package com.waste2value.pickup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePickupRequest(
        @NotNull(message = "Proposed price is required")
        @DecimalMin(value = "0.0", message = "Proposed price cannot be negative")
        BigDecimal proposedPrice,

        @Size(max = 1000, message = "Note cannot exceed 1000 characters")
        String note
) {}
