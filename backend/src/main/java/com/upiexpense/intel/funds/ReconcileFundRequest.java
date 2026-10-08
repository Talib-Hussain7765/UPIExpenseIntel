package com.upiexpense.intel.funds;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReconcileFundRequest(
        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal totalBalance,

        @NotNull
        LocalDate reconciledDate
)
{

}
