package com.upiexpense.intel.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        CategoryType categoryType,

        @Size(max = 20)
        String color,

        @Size(max = 80)
        String icon,

        Integer displayOrder
) {
}
