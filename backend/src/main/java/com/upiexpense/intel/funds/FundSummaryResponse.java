package com.upiexpense.intel.funds;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FundSummaryResponse(
        BigDecimal totalBalance,
        BigDecimal allocatedBalance,
        BigDecimal availableBalance,
        LocalDate reconciledDate,
        List<FundResponse> funds
) {
}
