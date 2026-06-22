package com.upiexpense.intel.category;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType categoryType,
        String color,
        String icon,
        int displayOrder,
        boolean defaultCategory,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType(),
                category.getColor(),
                category.getIcon(),
                category.getDisplayOrder(),
                category.isDefaultCategory(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
