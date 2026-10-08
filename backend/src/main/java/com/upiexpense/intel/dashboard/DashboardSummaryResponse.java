package com.upiexpense.intel.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record DashboardSummaryResponse(
        YearMonth month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal savingsAmount,
        BigDecimal savingsRate,
        BigDecimal upiDebitTotal,
        BigDecimal upiCreditTotal,
        int transactionCount,
        int uncategorizedCount,
        List<CategoryBreakdownItem> categoryBreakdown,
        List<MerchantBreakdownItem> topMerchants,
        List<DailyExpenseTrendItem> dailyExpenseTrend,
        BigDecimal closingBalance
) {

    public record CategoryBreakdownItem(
            UUID categoryId,
            String categoryName,
            String color,
            BigDecimal amount,
            int transactionCount
    ) {
    }

    public record MerchantBreakdownItem(
            String merchantName,
            BigDecimal amount,
            int transactionCount
    ) {
    }

    public record DailyExpenseTrendItem(
            LocalDate date,
            BigDecimal amount,
            int transactionCount
    ) {
    }
}
