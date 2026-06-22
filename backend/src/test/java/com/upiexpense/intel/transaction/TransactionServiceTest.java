package com.upiexpense.intel.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.category.CategoryType;
import com.upiexpense.intel.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategorizationService categorizationService;

    private TransactionService service;
    private UUID userId;
    private User user;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, categoryRepository, categorizationService);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        bankAccount = new BankAccount();
        bankAccount.setId(UUID.randomUUID());
        bankAccount.setUser(user);
    }

    @Test
    void listsTransactionsForMonthWithFilters() {
        Category food = category("Food", CategoryType.EXPENSE);
        Transaction foodDebit = transaction("Swiggy order", TransactionType.DEBIT, PaymentMode.UPI, food);
        foodDebit.setMerchantName("Swiggy");
        Transaction salaryCredit = transaction("Salary", TransactionType.CREDIT, PaymentMode.NEFT, null);
        when(transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(foodDebit, salaryCredit));

        List<TransactionResponse> responses = service.list(
                userId,
                "2026-04",
                TransactionType.DEBIT,
                PaymentMode.UPI,
                food.getId(),
                "swiggy",
                false,
                false
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().description()).isEqualTo("Swiggy order");
        assertThat(responses.getFirst().category().name()).isEqualTo("Food");
    }

    @Test
    void updatesCategoryTransferAndIgnoredFlags() {
        Transaction transaction = transaction("UPI transfer", TransactionType.DEBIT, PaymentMode.UPI, null);
        Category transfer = category("Transfer", CategoryType.TRANSFER);
        when(transactionRepository.findByIdAndUserId(transaction.getId(), userId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(transfer.getId(), userId)).thenReturn(Optional.of(transfer));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        TransactionResponse response = service.update(
                userId,
                transaction.getId(),
                new UpdateTransactionRequest(transfer.getId(), true, true)
        );

        assertThat(response.category().name()).isEqualTo("Transfer");
        assertThat(response.transfer()).isTrue();
        assertThat(response.ignored()).isTrue();
        verify(transactionRepository).save(transaction);
    }

    private Transaction transaction(
            String description,
            TransactionType transactionType,
            PaymentMode paymentMode,
            Category category
    ) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUser(user);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionDate(LocalDate.of(2026, 4, 12));
        transaction.setValueDate(LocalDate.of(2026, 4, 12));
        transaction.setDescription(description);
        transaction.setRawDescription(description);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionType(transactionType);
        transaction.setPaymentMode(paymentMode);
        transaction.setCategory(category);
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
}
