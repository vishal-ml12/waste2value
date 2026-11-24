package com.waste2value.category.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
