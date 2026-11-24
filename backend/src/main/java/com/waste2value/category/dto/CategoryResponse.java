package com.waste2value.category.dto;

import com.waste2value.category.Category;
import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long parentId,
        String parentName,
        String name,
        String slug,
        String description,
        String unit,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(Category category) {
        Long pId = category.getParentCategory() != null ? category.getParentCategory().getId() : null;
        String pName = category.getParentCategory() != null ? category.getParentCategory().getName() : null;
        return new CategoryResponse(
                category.getId(),
                pId,
                pName,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getUnit(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
