package com.upiexpense.intel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryType;
import com.upiexpense.intel.transaction.PaymentMode;
import com.upiexpense.intel.transaction.Transaction;
import com.upiexpense.intel.transaction.TransactionRepository;
import com.upiexpense.intel.transaction.TransactionType;
import com.upiexpense.intel.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private DashboardService service;
    private UUID userId;
    private User user;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        service = new DashboardService(transactionRepository);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        bankAccount = new BankAccount();
        bankAccount.setId(UUID.randomUUID());
        bankAccount.setUser(user);
    }

    @Test
    void buildsMonthlySummaryFromNonIgnoredNonTransferTransactions() {
        Category food = category("Food", "#F97316");
        Category bills = category("Bills", "#EAB308");
        Transaction salary = transaction("Salary", "Employer", new BigDecimal("100000.00"), TransactionType.CREDIT, PaymentMode.NEFT, null);
        Transaction swiggy = transaction("Swiggy", "Swiggy", new BigDecimal("500.00"), TransactionType.DEBIT, PaymentMode.UPI, food);
        Transaction electricity = transaction("Electricity", "BESCOM", new BigDecimal("1500.00"), TransactionType.DEBIT, PaymentMode.UPI, bills);
        Transaction uncategorized = transaction("Unknown", null, new BigDecimal("250.00"), TransactionType.DEBIT, PaymentMode.CARD, null);
        Transaction ignored = transaction("Ignored", "Ignored", new BigDecimal("1000.00"), TransactionType.DEBIT, PaymentMode.UPI, food);
        ignored.setIgnored(true);
        Transaction transfer = transaction("Transfer", "Transfer", new BigDecimal("2000.00"), TransactionType.DEBIT, PaymentMode.UPI, null);
        transfer.setTransfer(true);

        when(transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(salary, swiggy, electricity, uncategorized, ignored, transfer));

        DashboardSummaryResponse response = service.summary(userId, "2026-04");

        assertThat(response.month()).isEqualTo(YearMonth.of(2026, 4));
        assertThat(response.totalIncome()).isEqualByComparingTo("100000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("2250.00");
        assertThat(response.savingsAmount()).isEqualByComparingTo("97750.00");
        assertThat(response.savingsRate()).isEqualByComparingTo("97.75");
        assertThat(response.upiDebitTotal()).isEqualByComparingTo("2000.00");
        assertThat(response.upiCreditTotal()).isEqualByComparingTo("0");
        assertThat(response.transactionCount()).isEqualTo(4);
        assertThat(response.uncategorizedCount()).isEqualTo(2);
        assertThat(response.categoryBreakdown()).extracting(DashboardSummaryResponse.CategoryBreakdownItem::categoryName)
                .containsExactly("Bills", "Food", "Uncategorized");
        assertThat(response.topMerchants()).extracting(DashboardSummaryResponse.MerchantBreakdownItem::merchantName)
                .containsExactly("BESCOM", "Swiggy", "Unknown");
        assertThat(response.dailyExpenseTrend()).hasSize(30);
        assertThat(response.dailyExpenseTrend().get(11).amount()).isEqualByComparingTo("2250.00");
    }

    private Transaction transaction(
            String description,
            String merchantName,
            BigDecimal amount,
            TransactionType transactionType,
            PaymentMode paymentMode,
            Category category
    ) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUser(user);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionDate(LocalDate.of(2026, 4, 12));
        transaction.setDescription(description);
        transaction.setRawDescription(description);
        transaction.setMerchantName(merchantName);
        transaction.setAmount(amount);
        transaction.setTransactionType(transactionType);
        transaction.setPaymentMode(paymentMode);
        transaction.setCategory(category);
        return transaction;
    }

    private Category category(String name, String color) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setCategoryType(CategoryType.EXPENSE);
        category.setColor(color);
        return category;
    }
}
