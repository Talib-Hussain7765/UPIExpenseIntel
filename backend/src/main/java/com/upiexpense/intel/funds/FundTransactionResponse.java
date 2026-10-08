package com.upiexpense.intel.funds;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FundTransactionResponse(
        UUID id,
        UUID fundId,
        String fundName,
        BigDecimal amount,
        FundTransactionType type,
        LocalDate transactionDate,
        String note,
        OffsetDateTime createdAt
) {
}
