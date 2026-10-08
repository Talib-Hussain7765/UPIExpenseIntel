package com.upiexpense.intel.upload;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.bankaccount.BankAccountRepository;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import com.upiexpense.intel.parser.*;
import com.upiexpense.intel.transaction.CategorizationService;
import com.upiexpense.intel.transaction.Transaction;
import com.upiexpense.intel.transaction.TransactionRepository;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StatementUploadService {

    private static final Logger log = LoggerFactory.getLogger(StatementUploadService.class);
    private static final String PDF_FILE_TYPE = "PDF";

    private final StatementUploadRepository statementUploadRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final StatementParserSelector statementParserSelector;
    private final CategorizationService categorizationService;

    public StatementUploadService(
            StatementUploadRepository statementUploadRepository,
            TransactionRepository transactionRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            PdfTextExtractor pdfTextExtractor,
            StatementParserSelector statementParserSelector,
            CategorizationService categorizationService
    ) {
        this.statementUploadRepository = statementUploadRepository;
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.statementParserSelector = statementParserSelector;
        this.categorizationService = categorizationService;
    }

    @Transactional
    public StatementUploadResponse uploadStatement(
            UUID userId,
            UUID bankAccountId,
            String statementMonth,
            MultipartFile file,
            String pdfPassword,
            StatementImportDateBasis dateBasis,
            String importStartDate,
            String importEndDate
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Statement file is required");
        }

        String originalFileName = file.getOriginalFilename() == null ? "statement.pdf" : file.getOriginalFilename();
        String fileType = fileType(originalFileName);
        if (!PDF_FILE_TYPE.equals(fileType)) {
            throw new BadRequestException("Only PDF statements are supported for this bank parser");
        }

        byte[] fileBytes = readFileBytes(file);
        String fileSha256 = sha256(fileBytes);
        LocalDate parsedStatementMonth = parseStatementMonth(statementMonth);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        BankAccount bankAccount = bankAccountRepository.findByIdAndUserIdAndActiveTrue(bankAccountId, userId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        if (statementUploadRepository.existsByUserIdAndBankAccountIdAndStatementMonthAndFileSha256(
                userId,
                bankAccountId,
                parsedStatementMonth,
                fileSha256
        )) {
            throw new BadRequestException("This statement file has already been uploaded for the selected account and month");
        }

        StatementUpload upload = new StatementUpload();
        upload.setUser(user);
        upload.setBankAccount(bankAccount);
        upload.setOriginalFileName(originalFileName);
        upload.setFileType(fileType);
        upload.setFileSha256(fileSha256);
        upload.setStatementMonth(parsedStatementMonth);
        upload.setStatus(UploadStatus.PARSING);
        upload.setOpeningBalance(BigDecimal.ZERO);
        StatementUpload savedUpload = statementUploadRepository.save(upload);

        try {
            String extractedText = pdfTextExtractor.extract(fileBytes, pdfPassword);
            StatementParseContext context = new StatementParseContext(
                    bankAccount.getBankName(),
                    fileType,
                    extractedText,
                    parsedStatementMonth,
                    dateBasis,
                    null,
                    null
            );
            StatementParser parser = statementParserSelector.select(context);
            StatementParseResult result = parser.parse(context);

            List<ParsedTransactionRow> importableRows = filterRowsByDateBasis(
                    result.transactions(),
                    parsedStatementMonth,
                    dateBasis,
                    importStartDate,
                    importEndDate
            );

            Set<TransactionSignature> knownTransactions = existingTransactionSignatures(
                    userId,
                    bankAccountId,
                    importableRows,
                    parsedStatementMonth
            );
            List<Transaction> transactions = new ArrayList<>();
            int duplicateRows = result.duplicateRows();
            for (ParsedTransactionRow row : importableRows) {
                if (knownTransactions.add(TransactionSignature.from(row))) {
                    transactions.add(toTransaction(user, bankAccount, savedUpload, row));
                } else {
                    duplicateRows++;
                }
            }
            int skippedRows = result.skippedRows() + result.transactions().size() - importableRows.size();
            categorizationService.categorize(userId, transactions, false);
            transactionRepository.saveAll(transactions);

            savedUpload.setTotalRows(result.totalRows());
            savedUpload.setOpeningBalance(result.openingBalance());
            savedUpload.setParsedRows(transactions.size());
            savedUpload.setSkippedRows(skippedRows);
            savedUpload.setDuplicateRows(duplicateRows);
            savedUpload.setFailedRows(result.failedRows());
            savedUpload.setStatus(result.failedRows() > 0 ? UploadStatus.PARTIALLY_FAILED : UploadStatus.PARSED);
            savedUpload.setErrorMessage(result.errors().isEmpty() ? null : String.join("; ", result.errors()));

            log.info("Statement parsed: userId={}, uploadId={}, bankAccountId={}, parsedRows={}, failedRows={}, OpeningBalance={}",
                    userId,
                    savedUpload.getId(),
                    bankAccountId,
                    savedUpload.getParsedRows(),
                    savedUpload.getFailedRows(),
                    savedUpload.getOpeningBalance()
            );
            return StatementUploadResponse.from(statementUploadRepository.save(savedUpload));
        } catch (BadRequestException exception) {
            markFailed(savedUpload, exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            markFailed(savedUpload, exception.getMessage());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<StatementUploadResponse> list(UUID userId, String statementMonth) {
        List<StatementUpload> uploads;
        if (statementMonth == null || statementMonth.isBlank()) {
            uploads = statementUploadRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            uploads = statementUploadRepository.findByUserIdAndStatementMonthOrderByCreatedAtDesc(
                    userId,
                    parseStatementMonth(statementMonth)
            );
        }
        return uploads.stream()
                .map(StatementUploadResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementUploadResponse get(UUID userId, UUID uploadId) {
        return StatementUploadResponse.from(findUpload(userId, uploadId));
    }

    @Transactional
    public void delete(UUID userId, UUID uploadId) {
        StatementUpload upload = findUpload(userId, uploadId);
        transactionRepository.deleteByUserIdAndStatementUploadId(userId, uploadId);
        statementUploadRepository.delete(upload);
        log.info("Bank Statement deleted for userId={}, statement_id={}", userId, uploadId);
    }

    private Transaction toTransaction(User user, BankAccount bankAccount, StatementUpload upload, ParsedTransactionRow row) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setBankAccount(bankAccount);
        transaction.setStatementUpload(upload);
        transaction.setTransactionDate(row.transactionDate());
        transaction.setValueDate(row.transactionDate());
        transaction.setDescription(row.description());
        transaction.setRawDescription(row.rawDescription());
        transaction.setAmount(row.amount());
        transaction.setTransactionType(row.transactionType());
        transaction.setPaymentMode(row.paymentMode());
        transaction.setMerchantName(row.merchantName());
        transaction.setBalanceAfterTransaction(row.balanceAfterTransaction());
        transaction.setReferenceNumber(row.referenceNumber());
        return transaction;
    }

    private Set<TransactionSignature> existingTransactionSignatures(
            UUID userId,
            UUID bankAccountId,
            List<ParsedTransactionRow> parsedRows,
            LocalDate fallbackStatementMonth
    ) {
        LocalDate startDate = parsedRows.stream()
                .map(ParsedTransactionRow::transactionDate)
                .min(LocalDate::compareTo)
                .orElse(fallbackStatementMonth);

        LocalDate endDate = parsedRows.stream()
                        .map(ParsedTransactionRow::transactionDate)
                        .max(LocalDate::compareTo)
                        .orElse(fallbackStatementMonth.plusMonths(1).minusDays(1));

        List<Transaction> existingTransactions =
                transactionRepository.findByUserIdAndBankAccountIdAndTransactionDateBetween(
                        userId,
                        bankAccountId,
                        startDate,
                        endDate
                );

        Set<TransactionSignature> signatures = new HashSet<>();
        for (Transaction transaction : existingTransactions) {
            signatures.add(TransactionSignature.from(transaction));
        }
        return signatures;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read statement file");
        }
    }

    private String sha256(byte[] fileBytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fileBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private void markFailed(StatementUpload upload, String message) {
        upload.setStatus(UploadStatus.FAILED);
        upload.setErrorMessage(message);
        statementUploadRepository.save(upload);
        log.warn("Statement upload failed: uploadId={}, message={}", upload.getId(), message);
    }

    private StatementUpload findUpload(UUID userId, UUID uploadId) {
        return statementUploadRepository.findByIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new NotFoundException("Statement upload not found"));
    }

    private LocalDate parseStatementMonth(String statementMonth) {
        try {
            return YearMonth.parse(statementMonth).atDay(1);
        } catch (RuntimeException exception) {
            throw new BadRequestException("statementMonth must use yyyy-MM format");
        }
    }

    private String fileType(String originalFileName) {
        if (originalFileName.toLowerCase().endsWith(".pdf")) {
            return PDF_FILE_TYPE;
        }
        throw new BadRequestException("Unsupported statement file type");
    }

    private record TransactionSignature(
            LocalDate transactionDate,
            String rawDescription,
            String amount,
            String transactionType,
            String referenceNumber,
            String balanceAfterTransaction
    ) {

        static TransactionSignature from(ParsedTransactionRow row) {
            return new TransactionSignature(
                    row.transactionDate(),
                    normalize(row.rawDescription()),
                    row.amount().stripTrailingZeros().toPlainString(),
                    row.transactionType().name(),
                    normalize(row.referenceNumber()),
                    row.balanceAfterTransaction() == null
                            ? null
                            : row.balanceAfterTransaction().stripTrailingZeros().toPlainString()
            );
        }

        static TransactionSignature from(Transaction transaction) {
            return new TransactionSignature(
                    transaction.getTransactionDate(),
                    normalize(transaction.getRawDescription()),
                    transaction.getAmount().stripTrailingZeros().toPlainString(),
                    transaction.getTransactionType().name(),
                    normalize(transaction.getReferenceNumber()),
                    transaction.getBalanceAfterTransaction() == null
                            ? null
                            : transaction.getBalanceAfterTransaction().stripTrailingZeros().toPlainString()
            );
        }

        private static String normalize(String value) {
            return value == null ? null : value.trim().replaceAll("\\s+", " ");
        }
    }

    private List<ParsedTransactionRow> filterRowsByDateBasis(
            List<ParsedTransactionRow> rows,
            LocalDate statementMonth,
            StatementImportDateBasis dateBasis,
            String importStartDate,
            String importEndDate
    ) {
        StatementImportDateBasis effectiveDateBasis =
                dateBasis == null ? StatementImportDateBasis.TRANSACTION_DATE : dateBasis;

        return switch (effectiveDateBasis) {
            case TRANSACTION_DATE -> rows;
            case STATEMENT_MONTH_ONLY -> rows.stream()
                    .filter(row -> row.transactionDate().withDayOfMonth(1).equals(statementMonth))
                    .toList();
            case CUSTOM_RANGE -> {
                LocalDate startDate = parseImportDate(importStartDate, "importStartDate");
                LocalDate endDate = parseImportDate(importEndDate, "importEndDate");
                if (endDate.isBefore(startDate)) {
                    throw new BadRequestException("importEndDate must be on or after importStartDate");
                }
                yield rows.stream()
                        .filter(row -> !row.transactionDate().isBefore(startDate)
                                && !row.transactionDate().isAfter(endDate))
                        .toList();
            }
        };
    }

    private LocalDate parseImportDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required when dateBasis is CUSTOM_RANGE");
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new BadRequestException(fieldName + " must use yyyy-MM-dd format");
        }
    }
}
