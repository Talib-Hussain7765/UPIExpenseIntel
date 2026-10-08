package com.upiexpense.intel.parser;

import java.time.LocalDate;

public record StatementParseContext(
        String bankName,
        String fileType,
        String extractedText,
        LocalDate statementMonth,
        StatementImportDateBasis dateBasis,
        LocalDate importStartDate,
        LocalDate importEndDate
) {
}
