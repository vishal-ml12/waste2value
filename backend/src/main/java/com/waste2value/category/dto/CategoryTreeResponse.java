package com.waste2value.category.dto;

import com.waste2value.category.Category;
import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String name,
        String slug,
        String description,
        String unit,
        boolean active,
        List<CategoryTreeResponse> subCategories
) {
    public static CategoryTreeResponse from(Category category, boolean activeOnly) {
        List<CategoryTreeResponse> subs = category.getSubCategories().stream()
                .filter(sub -> !activeOnly || sub.isActive())
                .map(sub -> CategoryTreeResponse.from(sub, activeOnly))
                .toList();
        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getUnit(),
                category.isActive(),
                subs
        );
    }
}
