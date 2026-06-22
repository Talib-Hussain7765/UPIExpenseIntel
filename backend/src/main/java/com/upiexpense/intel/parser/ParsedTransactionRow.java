package com.upiexpense.intel.parser;

import com.upiexpense.intel.transaction.PaymentMode;
import com.upiexpense.intel.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransactionRow(
        LocalDate transactionDate,
        String description,
        String rawDescription,
        BigDecimal amount,
        TransactionType transactionType,
        PaymentMode paymentMode,
        String merchantName,
        BigDecimal balanceAfterTransaction,
        String referenceNumber
) {
}
