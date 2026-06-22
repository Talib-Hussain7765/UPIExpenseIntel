package com.upiexpense.intel.transaction;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID bankAccountId,
        UUID statementUploadId,
        LocalDate transactionDate,
        LocalDate valueDate,
        String description,
        String rawDescription,
        BigDecimal amount,
        TransactionType transactionType,
        PaymentMode paymentMode,
        String merchantName,
        TransactionCategoryResponse category,
        BigDecimal balanceAfterTransaction,
        String referenceNumber,
        boolean recurring,
        boolean transfer,
        boolean ignored,
        BigDecimal categorizationConfidence,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getBankAccount().getId(),
                transaction.getStatementUpload() == null ? null : transaction.getStatementUpload().getId(),
                transaction.getTransactionDate(),
                transaction.getValueDate(),
                transaction.getDescription(),
                transaction.getRawDescription(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getPaymentMode(),
                transaction.getMerchantName(),
                TransactionCategoryResponse.from(transaction.getCategory()),
                transaction.getBalanceAfterTransaction(),
                transaction.getReferenceNumber(),
                transaction.isRecurring(),
                transaction.isTransfer(),
                transaction.isIgnored(),
                transaction.getCategorizationConfidence(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    public record TransactionCategoryResponse(
            UUID id,
            String name,
            CategoryType categoryType,
            String color,
            String icon
    ) {

        static TransactionCategoryResponse from(Category category) {
            if (category == null) {
                return null;
            }
            return new TransactionCategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getCategoryType(),
                    category.getColor(),
                    category.getIcon()
            );
        }
    }
}
