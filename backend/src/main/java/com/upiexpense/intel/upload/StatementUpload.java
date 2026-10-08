package com.upiexpense.intel.upload;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.common.domain.BaseEntity;
import com.upiexpense.intel.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "statement_uploads")
public class StatementUpload extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_path", length = 500)
    private String storedFilePath;

    @Column(name = "file_type", nullable = false, length = 30)
    private String fileType;

    @Column(name = "file_sha256", length = 64)
    private String fileSha256;

    @Column(name = "statement_month", nullable = false)
    private LocalDate statementMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UploadStatus status = UploadStatus.UPLOADED;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "parsed_rows", nullable = false)
    private int parsedRows;

    @Column(name = "skipped_rows", nullable = false)
    private int skippedRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "opening_balance", nullable = true, columnDefinition = "decimal(19,2) default 0.00")
    private BigDecimal openingBalance;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;


}
