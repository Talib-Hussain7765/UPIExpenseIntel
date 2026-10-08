package com.upiexpense.intel.parser;

import com.upiexpense.intel.transaction.PaymentMode;
import com.upiexpense.intel.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IciciPdfStatementParser implements StatementParser {

    private static final String ICICI_HEADER = "DATE MODE** PARTICULARS DEPOSITS WITHDRAWALS BALANCE";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Pattern DATE_LINE = Pattern.compile("^(\\d{2}-\\d{2}-\\d{4})(?:\\s+(.*))?$");
    private static final Pattern MONEY = Pattern.compile("\\d{1,3}(?:,\\d{2,3})*\\.\\d{2}|\\d+\\.\\d{2}");
    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{6,}\\b");
    private BigDecimal openingBalance = new BigDecimal("0.00");

    @Override
    public boolean supports(StatementParseContext context) {
        String bankName = normalize(context.bankName());
        String text = normalize(context.extractedText());
        return "PDF".equalsIgnoreCase(context.fileType())
                && bankName.contains("ICICI")
                && text.contains(ICICI_HEADER)
                && text.contains("STATEMENT OF TRANSACTIONS IN SAVINGS ACCOUNT");
    }

    @Override
    public StatementParseResult parse(StatementParseContext context) {
        List<String> lines = normalizedLines(context.extractedText());
        int headerIndex = findHeaderIndex(lines);
        if (headerIndex < 0) {
            return new StatementParseResult(List.of(), 0, 1, List.of("ICICI transaction table header not found"));
        }

        List<List<String>> blocks = transactionBlocks(lines.subList(headerIndex + 1, lines.size()));
        List<ParsedTransactionRow> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BigDecimal previousBalance = null;

        for (List<String> block : blocks) {
            try {
                ParsedBlock parsedBlock = parseBlock(block, previousBalance);
                previousBalance = parsedBlock.balanceAfterTransaction();
                parsedBlock.transaction().ifPresent(transactions::add);
            } catch (RuntimeException exception) {
                errors.add("Unable to parse transaction block starting with: " + block.getFirst());
            }
        }
        log.info("Parsed {} transactions, {} errors, closing balance: {}", transactions.size(), errors.size(), openingBalance);
        return new StatementParseResult(transactions, blocks.size(), errors.size(), errors, 0, 0, openingBalance);
    }

    private ParsedBlock parseBlock(List<String> block, BigDecimal previousBalance) {
        Matcher dateMatcher = DATE_LINE.matcher(block.getFirst());
        if (!dateMatcher.matches()) {
            return new ParsedBlock(java.util.Optional.empty(), previousBalance);
        }

        LocalDate transactionDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMAT);
        String firstLineRemainder = dateMatcher.group(2);
        String rawDescription = descriptionFrom(block, firstLineRemainder);
        String normalizedDescription = normalizeSpaces(rawDescription);
        String upperDescription = normalizedDescription.toUpperCase(Locale.ROOT);
        List<BigDecimal> moneyValues = moneyValues(block);
        BigDecimal balance = moneyValues.isEmpty() ? previousBalance : moneyValues.getLast();

        if (isCarryForwardBalance(firstLineRemainder) || isCarryForwardBalance(upperDescription)) {
            this.openingBalance = balance;
            return new ParsedBlock(java.util.Optional.empty(), balance);
        }

        AmountAndType amountAndType = amountAndType(moneyValues, previousBalance, balance, upperDescription);

        return new ParsedBlock(java.util.Optional.of(new ParsedTransactionRow(
                transactionDate,
                normalizedDescription,
                rawDescription,
                amountAndType.amount(),
                amountAndType.transactionType(),
                inferPaymentMode(upperDescription),
                inferMerchantName(normalizedDescription),
                balance,
                inferReferenceNumber(normalizedDescription)
        )), balance);
    }

    private List<List<String>> transactionBlocks(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : lines) {
            if (DATE_LINE.matcher(line).matches()) {
                if (!current.isEmpty()) {
                    blocks.add(current);
                }
                current = new ArrayList<>();
            }

            if (!current.isEmpty() || DATE_LINE.matcher(line).matches()) {
                current.add(line);
            }
        }

        if (!current.isEmpty()) {
            blocks.add(current);
        }
        return blocks;
    }

    private String descriptionFrom(List<String> block, String firstLineRemainder) {
        List<String> descriptionLines = new ArrayList<>();
        if (firstLineRemainder != null && !firstLineRemainder.isBlank() && !containsMoney(firstLineRemainder)) {
            descriptionLines.add(firstLineRemainder.trim());
        }

        for (int i = 1; i < block.size(); i++) {
            String line = block.get(i);
            if (isAmountOnlyLine(line)) {
                break;
            }
            descriptionLines.add(line);
        }

        return String.join(" ", descriptionLines);
    }

    private TransactionType inferTransactionType(String upperDescription) {
        if (upperDescription.contains("/CR/")
                || upperDescription.contains("PAYMENT FR")
                || upperDescription.contains("PAYMENT FROM")
                || upperDescription.contains(" FROM ")
                || upperDescription.contains(" SALARY")
                || upperDescription.startsWith("NEFT CR")
                || upperDescription.startsWith("IMPS CR")) {
            return TransactionType.CREDIT;
        }

        if (upperDescription.contains("/DR/")
                || upperDescription.contains("PAY TO")
                || upperDescription.contains(" TO ")
                || upperDescription.startsWith("ATM")
                || upperDescription.startsWith("POS")
                || upperDescription.startsWith("UPI/")
                || upperDescription.startsWith("ACH/")
                || upperDescription.startsWith("VPS/")) {
            return TransactionType.DEBIT;
        }

        throw new IllegalArgumentException("Transaction type could not be inferred");
    }

    private AmountAndType amountAndType(
            List<BigDecimal> moneyValues,
            BigDecimal previousBalance,
            BigDecimal balance,
            String upperDescription
    ) {
        if (previousBalance != null && balance != null) {
            BigDecimal balanceMovement = balance.subtract(previousBalance);
            int direction = balanceMovement.signum();
            if (direction > 0) {
                return new AmountAndType(balanceMovement.abs(), TransactionType.CREDIT);
            }
            if (direction < 0) {
                return new AmountAndType(balanceMovement.abs(), TransactionType.DEBIT);
            }
        }

        if (moneyValues.size() < 2) {
            throw new IllegalArgumentException("Transaction amount and balance not found");
        }

        return new AmountAndType(
                moneyValues.get(moneyValues.size() - 2),
                inferTransactionType(upperDescription)
        );
    }

    private boolean isCarryForwardBalance(String value) {
        String normalized = normalize(value);
        return normalized.equals("B/F")
                || normalized.equals("C/F")
                || normalized.startsWith("B/F ")
                || normalized.startsWith("C/F ");
    }

    private PaymentMode inferPaymentMode(String upperDescription) {
        if (upperDescription.contains("UPI")) {
            return PaymentMode.UPI;
        }
        if (upperDescription.contains("NEFT")) {
            return PaymentMode.NEFT;
        }
        if (upperDescription.contains("IMPS")) {
            return PaymentMode.IMPS;
        }
        if (upperDescription.startsWith("ATM")) {
            return PaymentMode.ATM;
        }
        if (upperDescription.startsWith("POS") || upperDescription.startsWith("VPS")) {
            return PaymentMode.POS;
        }
        if (upperDescription.contains("CHARG")) {
            return PaymentMode.CHARGES;
        }
        if (upperDescription.contains("INTEREST")) {
            return PaymentMode.INTEREST;
        }
        return PaymentMode.UNKNOWN;
    }

    private String inferMerchantName(String description) {
        String[] parts = description.split("/");
        if (parts.length >= 2 && parts[0].equalsIgnoreCase("UPI")) {
            return cleanMerchant(parts[1]);
        }
        return null;
    }

    private String inferReferenceNumber(String description) {
        Matcher matcher = LONG_NUMBER.matcher(description);
        return matcher.find() ? matcher.group() : null;
    }

    private String cleanMerchant(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9 ._-]", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private List<BigDecimal> moneyValues(List<String> block) {
        List<BigDecimal> values = new ArrayList<>();
        for (String line : block) {
            Matcher matcher = MONEY.matcher(line);
            while (matcher.find()) {
                values.add(parseMoney(matcher.group()));
            }
        }
        return values;
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(value.replace(",", ""));
    }

    private boolean containsMoney(String value) {
        return MONEY.matcher(value).find();
    }

    private boolean isAmountOnlyLine(String value) {
        String withoutMoney = MONEY.matcher(value).replaceAll("").trim();
        return withoutMoney.isBlank() && containsMoney(value);
    }

    private int findHeaderIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (normalize(lines.get(i)).equals(ICICI_HEADER)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> normalizedLines(String text) {
        return text.lines()
                .map(this::normalizeSpaces)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String normalize(String value) {
        return normalizeSpaces(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeSpaces(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private record ParsedBlock(java.util.Optional<ParsedTransactionRow> transaction, BigDecimal balanceAfterTransaction) {
    }

    private record AmountAndType(BigDecimal amount, TransactionType transactionType) {
    }
}
