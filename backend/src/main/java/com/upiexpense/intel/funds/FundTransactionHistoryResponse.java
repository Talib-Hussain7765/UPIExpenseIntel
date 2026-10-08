package com.upiexpense.intel.funds;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FundTransactionHistoryResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal totalAdded,
        BigDecimal totalWithdrawn,
        BigDecimal netMovement,
        List<FundTransactionResponse> transactions
) {
}
