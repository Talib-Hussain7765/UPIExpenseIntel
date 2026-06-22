package com.upiexpense.intel.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateMerchantRuleRequest(
        @NotBlank
        @Size(max = 255)
        String keyword,

        @NotNull
        RuleMatchType matchType,

        @NotNull
        UUID categoryId,

        @Size(max = 180)
        String merchantName,

        Integer priority,

        Boolean active
) {
}
