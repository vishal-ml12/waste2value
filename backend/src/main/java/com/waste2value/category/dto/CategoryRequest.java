package com.waste2value.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        Long parentId,

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Size(max = 100, message = "Slug must not exceed 100 characters")
        String slug,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        Boolean active
) {
}
