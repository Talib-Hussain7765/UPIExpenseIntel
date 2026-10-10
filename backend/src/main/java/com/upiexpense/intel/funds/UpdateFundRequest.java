package com.upiexpense.intel.funds;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFundRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        FundTransactionType type,

        @NotNull
        LocalDate transactionDate,

        String note,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal reconciledTotalBalance,

        @NotNull
        LocalDate reconciledDate
) {
}


