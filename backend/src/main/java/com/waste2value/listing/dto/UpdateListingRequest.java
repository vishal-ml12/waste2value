package com.waste2value.listing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateListingRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Address ID is required")
        Long addressId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @DecimalMin(value = "0.00", message = "Expected price cannot be negative")
        BigDecimal expectedPrice
) {
}
