package com.upiexpense.intel.bankaccount;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBankAccountRequest(
        @NotBlank
        @Size(max = 120)
        String bankName,

        @NotBlank
        @Size(max = 120)
        String accountNickname,

        @Size(max = 40)
        String maskedAccountNumber,

        @Size(max = 40)
        String accountType
) {
}
