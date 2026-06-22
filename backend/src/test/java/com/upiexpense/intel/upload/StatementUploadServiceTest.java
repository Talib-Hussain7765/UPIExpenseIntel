package com.upiexpense.intel.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.bankaccount.BankAccountRepository;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import com.upiexpense.intel.parser.ParsedTransactionRow;
import com.upiexpense.intel.parser.PdfTextExtractor;
import com.upiexpense.intel.parser.StatementParseContext;
import com.upiexpense.intel.parser.StatementParseResult;
import com.upiexpense.intel.parser.StatementParser;
import com.upiexpense.intel.parser.StatementParserSelector;
import com.upiexpense.intel.transaction.CategorizationService;
import com.upiexpense.intel.transaction.PaymentMode;
import com.upiexpense.intel.transaction.Transaction;
import com.upiexpense.intel.transaction.TransactionRepository;
import com.upiexpense.intel.transaction.TransactionType;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class StatementUploadServiceTest {

    @Mock
    private StatementUploadRepository statementUploadRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private StatementParserSelector statementParserSelector;

    @Mock
    private CategorizationService categorizationService;

    private StatementUploadService service;
    private UUID userId;
    private UUID bankAccountId;
    private User user;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        service = new StatementUploadService(
                statementUploadRepository,
                transactionRepository,
                bankAccountRepository,
                userRepository,
                pdfTextExtractor,
                statementParserSelector,
                categorizationService
        );
        userId = UUID.randomUUID();
        bankAccountId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        bankAccount = new BankAccount();
        bankAccount.setId(bankAccountId);
        bankAccount.setUser(user);
        bankAccount.setBankName("ICICI Bank");
    }

    @Test
    void rejectsSameFileForSameAccountAndMonth() {
        MockMultipartFile file = pdf("statement.pdf", "same-content");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findByIdAndUserIdAndActiveTrue(bankAccountId, userId))
                .thenReturn(Optional.of(bankAccount));
        when(statementUploadRepository.existsByUserIdAndBankAccountIdAndStatementMonthAndFileSha256(
                eq(userId),
                eq(bankAccountId),
                eq(LocalDate.of(2026, 4, 1)),
                anyString()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.uploadStatement(userId, bankAccountId, "2026-04", file, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This statement file has already been uploaded for the selected account and month");

        verify(statementUploadRepository, never()).save(any(StatementUpload.class));
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void skipsTransactionsAlreadyImportedForSameAccountAndMonth() throws Exception {
        MockMultipartFile file = pdf("statement.pdf", "new-content");
        ParsedTransactionRow duplicateRow = row(
                "UPI/SHOP/pay/111111",
                new BigDecimal("250.00"),
                TransactionType.DEBIT,
                new BigDecimal("750.00"),
                "111111"
        );
        ParsedTransactionRow newRow = row(
                "UPI/FRIEND/pay/222222",
                new BigDecimal("500.00"),
                TransactionType.CREDIT,
                new BigDecimal("1250.00"),
                "222222"
        );
        Transaction existingTransaction = transactionFrom(duplicateRow);
        StatementParser parser = new StatementParser() {
            @Override
            public boolean supports(StatementParseContext context) {
                return true;
            }

            @Override
            public StatementParseResult parse(StatementParseContext context) {
                return new StatementParseResult(List.of(duplicateRow, newRow), 2, 0, List.of());
            }
        };

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findByIdAndUserIdAndActiveTrue(bankAccountId, userId))
                .thenReturn(Optional.of(bankAccount));
        when(statementUploadRepository.save(any(StatementUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfTextExtractor.extract(file.getBytes(), null)).thenReturn("extracted text");
        when(statementParserSelector.select(any(StatementParseContext.class))).thenReturn(parser);
        when(transactionRepository.findByUserIdAndBankAccountIdAndTransactionDateBetween(
                userId,
                bankAccountId,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(existingTransaction));

        service.uploadStatement(userId, bankAccountId, "2026-04", file, null);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getRawDescription()).isEqualTo(newRow.rawDescription());
        assertThat(captor.getValue().getFirst().getTransactionType()).isEqualTo(TransactionType.CREDIT);
    }

    @Test
    void listsUploadsForRequestedMonth() {
        StatementUpload upload = upload(LocalDate.of(2026, 4, 1), UploadStatus.PARSED);
        when(statementUploadRepository.findByUserIdAndStatementMonthOrderByCreatedAtDesc(
                userId,
                LocalDate.of(2026, 4, 1)
        )).thenReturn(List.of(upload));

        List<StatementUploadResponse> responses = service.list(userId, "2026-04");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(upload.getId());
        assertThat(responses.getFirst().status()).isEqualTo(UploadStatus.PARSED);
    }

    @Test
    void getsUploadOnlyWhenOwnedByCurrentUser() {
        UUID uploadId = UUID.randomUUID();
        when(statementUploadRepository.findByIdAndUserId(uploadId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, uploadId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Statement upload not found");
    }

    @Test
    void deletesOwnedUploadAndItsTransactions() {
        StatementUpload upload = upload(LocalDate.of(2026, 4, 1), UploadStatus.PARSED);
        when(statementUploadRepository.findByIdAndUserId(upload.getId(), userId)).thenReturn(Optional.of(upload));

        service.delete(userId, upload.getId());

        verify(transactionRepository).deleteByUserIdAndStatementUploadId(userId, upload.getId());
        verify(statementUploadRepository).delete(upload);
    }

    private MockMultipartFile pdf(String originalFilename, String content) {
        return new MockMultipartFile("file", originalFilename, "application/pdf", content.getBytes());
    }

    private ParsedTransactionRow row(
            String rawDescription,
            BigDecimal amount,
            TransactionType transactionType,
            BigDecimal balance,
            String referenceNumber
    ) {
        return new ParsedTransactionRow(
                LocalDate.of(2026, 4, 2),
                rawDescription,
                rawDescription,
                amount,
                transactionType,
                PaymentMode.UPI,
                null,
                balance,
                referenceNumber
        );
    }

    private Transaction transactionFrom(ParsedTransactionRow row) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionDate(row.transactionDate());
        transaction.setRawDescription(row.rawDescription());
        transaction.setAmount(row.amount());
        transaction.setTransactionType(row.transactionType());
        transaction.setReferenceNumber(row.referenceNumber());
        transaction.setBalanceAfterTransaction(row.balanceAfterTransaction());
        return transaction;
    }

    private StatementUpload upload(LocalDate statementMonth, UploadStatus status) {
        StatementUpload upload = new StatementUpload();
        upload.setId(UUID.randomUUID());
        upload.setUser(user);
        upload.setBankAccount(bankAccount);
        upload.setOriginalFileName("statement.pdf");
        upload.setFileType("PDF");
        upload.setStatementMonth(statementMonth);
        upload.setStatus(status);
        upload.setTotalRows(2);
        upload.setParsedRows(2);
        return upload;
    }
}
