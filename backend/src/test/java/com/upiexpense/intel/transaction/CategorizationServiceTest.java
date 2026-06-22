package com.upiexpense.intel.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.category.CategoryType;
import com.upiexpense.intel.rule.MerchantRule;
import com.upiexpense.intel.rule.MerchantRuleRepository;
import com.upiexpense.intel.rule.RuleMatchType;
import com.upiexpense.intel.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MerchantRuleRepository merchantRuleRepository;

    private CategorizationService service;
    private UUID userId;
    private User user;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        service = new CategorizationService(categoryRepository, merchantRuleRepository);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        bankAccount = new BankAccount();
        bankAccount.setId(UUID.randomUUID());
        bankAccount.setUser(user);
    }

    @Test
    void categorizesCommonIncomeAndExpenseTransactions() {
        Category salary = category("Salary", CategoryType.INCOME);
        Category food = category("Food", CategoryType.EXPENSE);
        Category otherExpense = category("Other Expense", CategoryType.EXPENSE);
        when(categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId))
                .thenReturn(List.of(salary, food, otherExpense));
        when(merchantRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(userId))
                .thenReturn(List.of());
        Transaction salaryCredit = transaction("NEFT CR SAL ACME PVT LTD", TransactionType.CREDIT, PaymentMode.NEFT);
        Transaction foodDebit = transaction("UPI/SWIGGY/order", TransactionType.DEBIT, PaymentMode.UPI);
        Transaction fallbackDebit = transaction("UPI/UNKNOWN/person", TransactionType.DEBIT, PaymentMode.UPI);

        int updated = service.categorize(userId, List.of(salaryCredit, foodDebit, fallbackDebit), false);

        assertThat(updated).isEqualTo(3);
        assertThat(salaryCredit.getCategory()).isEqualTo(salary);
        assertThat(foodDebit.getCategory()).isEqualTo(food);
        assertThat(fallbackDebit.getCategory()).isEqualTo(otherExpense);
    }

    @Test
    void doesNotOverwriteExistingCategoryUnlessRequested() {
        Category food = category("Food", CategoryType.EXPENSE);
        Category shopping = category("Shopping", CategoryType.EXPENSE);
        when(categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId))
                .thenReturn(List.of(food, shopping));
        when(merchantRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(userId))
                .thenReturn(List.of());
        Transaction transaction = transaction("AMAZON order", TransactionType.DEBIT, PaymentMode.UPI);
        transaction.setCategory(food);

        int updated = service.categorize(userId, List.of(transaction), false);

        assertThat(updated).isZero();
        assertThat(transaction.getCategory()).isEqualTo(food);

        updated = service.categorize(userId, List.of(transaction), true);

        assertThat(updated).isEqualTo(1);
        assertThat(transaction.getCategory()).isEqualTo(shopping);
    }

    @Test
    void appliesUserMerchantRuleBeforeFallbackRules() {
        Category travel = category("Travel", CategoryType.EXPENSE);
        Category otherExpense = category("Other Expense", CategoryType.EXPENSE);
        MerchantRule rule = rule("RAPIDO", RuleMatchType.CONTAINS, travel, "Rapido", 10);
        when(categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId))
                .thenReturn(List.of(travel, otherExpense));
        when(merchantRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(userId))
                .thenReturn(List.of(rule));
        Transaction transaction = transaction("UPI/unknown/rapido ride", TransactionType.DEBIT, PaymentMode.UPI);

        int updated = service.categorize(userId, List.of(transaction), false);

        assertThat(updated).isEqualTo(1);
        assertThat(transaction.getCategory()).isEqualTo(travel);
        assertThat(transaction.getMerchantName()).isEqualTo("Rapido");
        assertThat(transaction.getCategorizationConfidence()).isEqualByComparingTo("0.95");
    }

    private Transaction transaction(String description, TransactionType transactionType, PaymentMode paymentMode) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUser(user);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionDate(LocalDate.of(2026, 4, 12));
        transaction.setDescription(description);
        transaction.setRawDescription(description);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionType(transactionType);
        transaction.setPaymentMode(paymentMode);
        return transaction;
    }

    private Category category(String name, CategoryType categoryType) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setUser(user);
        category.setName(name);
        category.setCategoryType(categoryType);
        category.setActive(true);
        return category;
    }

    private MerchantRule rule(
            String keyword,
            RuleMatchType matchType,
            Category category,
            String merchantName,
            int priority
    ) {
        MerchantRule rule = new MerchantRule();
        rule.setId(UUID.randomUUID());
        rule.setUser(user);
        rule.setKeyword(keyword);
        rule.setMatchType(matchType);
        rule.setCategory(category);
        rule.setMerchantName(merchantName);
        rule.setPriority(priority);
        rule.setActive(true);
        return rule;
    }
}
