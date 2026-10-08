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

import com.upiexpense.intel.upload.StatementUpload;
import com.upiexpense.intel.upload.StatementUploadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DashboardService {

    private static final String UNCATEGORIZED = "Uncategorized";
    private static final String UNKNOWN_MERCHANT = "Unknown";

    private final TransactionRepository transactionRepository;
    private final StatementUploadRepository statementUploadRepository;

    public DashboardService(TransactionRepository transactionRepository, StatementUploadRepository statementUploadRepository) {
        this.transactionRepository = transactionRepository;
        this.statementUploadRepository = statementUploadRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UUID userId, LocalDate startDate, LocalDate endDate ) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userId,
                        startDate,
                        endDate
                )
                .stream()
                .filter(transaction -> !transaction.isIgnored())
                .filter(transaction -> !transaction.isTransfer())
                .toList();

        BigDecimal totalIncome = sum(transactions, TransactionType.CREDIT);
        BigDecimal totalExpenses = sum(transactions, TransactionType.DEBIT);

        transactions.stream().filter(transaction -> transaction.getTransactionDate().toString().equals( "2026-07-26"))
                .forEach(transaction -> log.info("Transaction on 2026-07-26: {} amount: {} balance: {}, createdAt: {}",
                        transaction.getDescription(), transaction.getAmount(), transaction.getBalanceAfterTransaction(),transaction.getCreatedAt()));
        

        BigDecimal savingsAmount = totalIncome.subtract(totalExpenses);
        BigDecimal closingBalance = getClosingBalance(userId, transactions, savingsAmount, startDate, endDate);
        log.info("Closing balance for user {} from {} to {} is {}", userId, startDate, endDate, closingBalance);
        BigDecimal savingsRate = totalIncome.signum() == 0
                ? BigDecimal.ZERO
                : savingsAmount.multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP);
        BigDecimal upiDebitTotal = sumByTypeAndPaymentMode(transactions, TransactionType.DEBIT, PaymentMode.UPI);
        BigDecimal upiCreditTotal = sumByTypeAndPaymentMode(transactions, TransactionType.CREDIT, PaymentMode.UPI);
        int uncategorizedCount = (int) transactions.stream()
                .filter(transaction -> transaction.getCategory() == null)
                .count();

        return new DashboardSummaryResponse(
                YearMonth.from(endDate),
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
                dailyExpenseTrend(transactions, startDate, endDate),
                closingBalance
        );
    }

    private BigDecimal getClosingBalance(UUID userId, List<Transaction> transactions, BigDecimal savingsAmount, LocalDate startDate, LocalDate endDate) {
        StatementUpload startDateUpload = transactions.stream().filter(transaction -> transaction.getTransactionDate().equals(startDate))
                .map(Transaction::getStatementUpload).findFirst().orElse(null);

        StatementUpload endDateUpload = transactions.stream().filter(transaction -> transaction.getTransactionDate().equals(endDate))
                .map(Transaction::getStatementUpload).findFirst().orElse(null);

        Transaction firstTransaction = null;
        if(startDateUpload!=null) {
            firstTransaction = transactionRepository.findFirstByUserIdAndStatementUploadIdOrderByTransactionDateAsc(userId, startDateUpload.getId()).orElseThrow(() -> new BadRequestException("No transactions found for the given start date upload"));
        }

        if(startDateUpload != null && startDateUpload.equals(endDateUpload) && startDate.equals(firstTransaction.getTransactionDate())) {
            log.info("Opening balance for upload id: {} is {}",startDateUpload.getId(), startDateUpload.getOpeningBalance());
            return startDateUpload.getOpeningBalance().add(savingsAmount);
        }

        return new BigDecimal(-1);
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
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("daily trend expense range: {} to {}", startDate, endDate);
        Map<LocalDate, Aggregate> aggregates = new LinkedHashMap<>();
        
        // Initialize all dates in the range
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            aggregates.put(date, new Aggregate());
        }

        // Add transaction amounts to the corresponding dates
        transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.DEBIT)
                .forEach(transaction -> {
                    LocalDate txDate = transaction.getTransactionDate();
                    if (aggregates.containsKey(txDate)) {
                        aggregates.get(txDate).add(transaction.getAmount());
                    }
                });

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
