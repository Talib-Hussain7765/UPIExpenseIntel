package com.upiexpense.intel.parser;

import java.util.List;

public record StatementParseResult(
        List<ParsedTransactionRow> transactions,
        int totalRows,
        int failedRows,
        List<String> errors
) {
}
