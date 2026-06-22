package com.upiexpense.intel.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.upiexpense.intel.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IciciPdfStatementParserTest {

    private final IciciPdfStatementParser parser = new IciciPdfStatementParser();

    @Test
    void parsesDebitAndCreditFromBalanceMovement() {
        String extractedText = """
                Statement of Transactions in Savings Account Number: 123456 in INR for the period April 01, 2026 - April 30, 2026
                DATE MODE** PARTICULARS DEPOSITS WITHDRAWALS BALANCE
                01-04-2026 B/F 1,000.00
                01-04-2026
                UPI/SHOP/paytmqr@bank/Payment fr/YES BANK/123456789012
                250.00 750.00
                02-04-2026
                UPI/FRIEND/mobile@bank/Payment fr/HDFC BANK/987654321098
                500.00 1,250.00
                """;
        StatementParseContext context = new StatementParseContext(
                "ICICI Bank",
                "PDF",
                extractedText,
                LocalDate.of(2026, 4, 1)
        );

        StatementParseResult result = parser.parse(context);

        assertThat(result.errors()).isEmpty();
        assertThat(result.transactions()).hasSize(2);
        assertThat(result.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.transactions().get(0).transactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.transactions().get(1).amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.transactions().get(1).transactionType()).isEqualTo(TransactionType.CREDIT);
    }
}
