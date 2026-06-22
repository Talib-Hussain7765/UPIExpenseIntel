package com.upiexpense.intel.dashboard;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.transaction.PaymentMode;
import com.upiexpense.intel.transaction.Transaction;
import com.upiexpense.intel.transaction.TransactionRepository;
import com.upiexpense.intel.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final String UNCATEGORIZED = "Uncategorized";
    private static final String UNKNOWN_MERCHANT = "Unknown";

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UUID userId, String month) {
        YearMonth yearMonth = parseMonth(month);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .filter(transaction -> !transaction.isIgnored())
                .filter(transaction -> !transaction.isTransfer())
                .toList();

        BigDecimal totalIncome = sum(transactions, TransactionType.CREDIT);
        BigDecimal totalExpenses = sum(transactions, TransactionType.DEBIT);
        BigDecimal savingsAmount = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = totalIncome.signum() == 0
                ? BigDecimal.ZERO
                : savingsAmount.multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP);
        BigDecimal upiDebitTotal = sumByTypeAndPaymentMode(transactions, TransactionType.DEBIT, PaymentMode.UPI);
        BigDecimal upiCreditTotal = sumByTypeAndPaymentMode(transactions, TransactionType.CREDIT, PaymentMode.UPI);
        int uncategorizedCount = (int) transactions.stream()
                .filter(transaction -> transaction.getCategory() == null)
                .count();

        return new DashboardSummaryResponse(
                yearMonth,
                totalIncome,
                totalExpenses,
                savingsAmount,
                savingsRate,
                upiDebitTotal,
                upiCreditTotal,
                transactions.size(),
                uncategorizedCount,
                categoryBreakdown(transactions),
                topMerchants(transactions),
                dailyExpenseTrend(transactions, yearMonth)
        );
    }

    private List<DashboardSummaryResponse.CategoryBreakdownItem> categoryBreakdown(List<Transaction> transactions) {
        Map<CategoryKey, Aggregate> aggregates = new LinkedHashMap<>();
        transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.DEBIT)
                .forEach(transaction -> {
                    Category category = transaction.getCategory();
                    CategoryKey key = category == null
                            ? new CategoryKey(null, UNCATEGORIZED, null)
                            : new CategoryKey(category.getId(), category.getName(), category.getColor());
                    aggregates.computeIfAbsent(key, ignored -> new Aggregate()).add(transaction.getAmount());
                });

        return aggregates.entrySet().stream()
                .map(entry -> new DashboardSummaryResponse.CategoryBreakdownItem(
                        entry.getKey().categoryId(),
                        entry.getKey().categoryName(),
                        entry.getKey().color(),
                        entry.getValue().amount(),
                        entry.getValue().count()
                ))
                .sorted(Comparator.comparing(DashboardSummaryResponse.CategoryBreakdownItem::amount).reversed())
                .toList();
    }

    private List<DashboardSummaryResponse.MerchantBreakdownItem> topMerchants(List<Transaction> transactions) {
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.DEBIT)
                .forEach(transaction -> {
                    String merchantName = merchantName(transaction);
                    aggregates.computeIfAbsent(merchantName, ignored -> new Aggregate()).add(transaction.getAmount());
                });

        return aggregates.entrySet().stream()
                .map(entry -> new DashboardSummaryResponse.MerchantBreakdownItem(
                        entry.getKey(),
                        entry.getValue().amount(),
                        entry.getValue().count()
                ))
                .sorted(Comparator.comparing(DashboardSummaryResponse.MerchantBreakdownItem::amount).reversed())
                .limit(10)
                .toList();
    }

    private List<DashboardSummaryResponse.DailyExpenseTrendItem> dailyExpenseTrend(
            List<Transaction> transactions,
            YearMonth yearMonth
    ) {
        Map<LocalDate, Aggregate> aggregates = new LinkedHashMap<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            aggregates.put(yearMonth.atDay(day), new Aggregate());
        }

        transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.DEBIT)
                .forEach(transaction -> aggregates.get(transaction.getTransactionDate()).add(transaction.getAmount()));

        return aggregates.entrySet().stream()
                .map(entry -> new DashboardSummaryResponse.DailyExpenseTrendItem(
                        entry.getKey(),
                        entry.getValue().amount(),
                        entry.getValue().count()
                ))
                .toList();
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType transactionType) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == transactionType)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByTypeAndPaymentMode(
            List<Transaction> transactions,
            TransactionType transactionType,
            PaymentMode paymentMode
    ) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == transactionType)
                .filter(transaction -> transaction.getPaymentMode() == paymentMode)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String merchantName(Transaction transaction) {
        if (transaction.getMerchantName() != null && !transaction.getMerchantName().isBlank()) {
            return transaction.getMerchantName().trim();
        }
        return UNKNOWN_MERCHANT;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new BadRequestException("month must use yyyy-MM format");
        }
    }

    private record CategoryKey(UUID categoryId, String categoryName, String color) {
    }

    private static class Aggregate {

        private BigDecimal amount = BigDecimal.ZERO;
        private int count;

        void add(BigDecimal value) {
            amount = amount.add(value);
            count++;
        }

        BigDecimal amount() {
            return amount;
        }

        int count() {
            return count;
        }
    }
}
