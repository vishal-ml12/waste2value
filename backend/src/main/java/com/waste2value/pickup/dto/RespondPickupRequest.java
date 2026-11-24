package com.waste2value.pickup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespondPickupRequest(
        @NotNull(message = "Decision (accept=true/false) is required")
        Boolean accept,

        @Size(max = 1000, message = "Note cannot exceed 1000 characters")
        String note
) {}
