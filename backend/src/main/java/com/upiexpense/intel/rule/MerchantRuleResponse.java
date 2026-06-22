package com.upiexpense.intel.rule;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MerchantRuleResponse(
        UUID id,
        String keyword,
        RuleMatchType matchType,
        RuleCategoryResponse category,
        String merchantName,
        int priority,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static MerchantRuleResponse from(MerchantRule rule) {
        return new MerchantRuleResponse(
                rule.getId(),
                rule.getKeyword(),
                rule.getMatchType(),
                RuleCategoryResponse.from(rule.getCategory()),
                rule.getMerchantName(),
                rule.getPriority(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    public record RuleCategoryResponse(
            UUID id,
            String name,
            CategoryType categoryType,
            String color,
            String icon
    ) {

        static RuleCategoryResponse from(Category category) {
            return new RuleCategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getCategoryType(),
                    category.getColor(),
                    category.getIcon()
            );
        }
    }
}
