package com.upiexpense.intel.transaction;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            CategorizationService categorizationService
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(
            UUID userId,
            String month,
            TransactionType transactionType,
            PaymentMode paymentMode,
            UUID categoryId,
            String merchant,
            Boolean ignored,
            Boolean transfer
    ) {
        YearMonth yearMonth = month == null || month.isBlank() ? YearMonth.now() : parseMonth(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userId,
                        startDate,
                        endDate
                )
                .stream()
                .filter(transaction -> transactionType == null || transaction.getTransactionType() == transactionType)
                .filter(transaction -> paymentMode == null || transaction.getPaymentMode() == paymentMode)
                .filter(transaction -> categoryId == null
                        || transaction.getCategory() != null && categoryId.equals(transaction.getCategory().getId()))
                .filter(transaction -> merchant == null || merchant.isBlank()
                        || containsIgnoreCase(transaction.getMerchantName(), merchant)
                        || containsIgnoreCase(transaction.getDescription(), merchant))
                .filter(transaction -> ignored == null || transaction.isIgnored() == ignored)
                .filter(transaction -> transfer == null || transaction.isTransfer() == transfer)
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID transactionId) {
        return TransactionResponse.from(findOwnedTransaction(userId, transactionId));
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, UpdateTransactionRequest request) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndUserIdAndActiveTrue(request.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            transaction.setCategory(category);
        }
        if (request.transfer() != null) {
            transaction.setTransfer(request.transfer());
        }
        if (request.ignored() != null) {
            transaction.setIgnored(request.ignored());
        }

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public CategorizationResponse categorizeMonth(UUID userId, String month, boolean overwrite) {
        YearMonth yearMonth = parseMonth(month);
        log.info("Categorize month requested: userId={}, month={}, overwrite={}", userId, yearMonth, overwrite);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
        int updated = categorizationService.categorize(userId, transactions, overwrite);
        transactionRepository.saveAll(transactions);
        log.info("Categorize month completed: userId={}, month={}, transactionCount={}, updatedTransactions={}, overwrite={}",
                userId,
                yearMonth,
                transactions.size(),
                updated,
                overwrite
        );
        return new CategorizationResponse(updated);
    }

    private Transaction findOwnedTransaction(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new BadRequestException("month must use yyyy-MM format");
        }
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search.toLowerCase());
    }
}
