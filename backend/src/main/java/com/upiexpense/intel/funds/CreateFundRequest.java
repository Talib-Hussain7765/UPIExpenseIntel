package com.upiexpense.intel.funds;

import jakarta.validation.constraints.NotBlank;

public record CreateFundRequest(
        @NotBlank String name
) {
}
