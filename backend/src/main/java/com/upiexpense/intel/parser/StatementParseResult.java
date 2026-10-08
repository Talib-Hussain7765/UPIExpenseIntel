package com.upiexpense.intel.parser;

import java.math.BigDecimal;
import java.util.List;

public record StatementParseResult(
        List<ParsedTransactionRow> transactions,
        int totalRows,
        int failedRows,
        List<String> errors,
        int skippedRows,
        int duplicateRows,
        java.math.BigDecimal openingBalance
) {

    public StatementParseResult(
            List<ParsedTransactionRow> transactions,
            int totalRows,
            int failedRows,
            List<String> errors
    ) {
        this(transactions, totalRows, failedRows, errors, 0, 0, BigDecimal.valueOf(0.00));
    }

    public StatementParseResult(
            List<ParsedTransactionRow> transactions,
            int totalRows,
            int failedRows,
            List<String> errors,
            int skippedRows,
            int duplicateRows
    ) {
        this(transactions, totalRows, failedRows, errors, skippedRows, duplicateRows, BigDecimal.valueOf(0));
    }
}
