package com.upiexpense.intel.transaction;

import com.upiexpense.intel.bankaccount.BankAccount;
import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.common.domain.BaseEntity;
import com.upiexpense.intel.upload.StatementUpload;
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
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_upload_id")
    private StatementUpload statementUpload;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "raw_description", nullable = false, length = 1500)
    private String rawDescription;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private PaymentMode paymentMode = PaymentMode.UNKNOWN;

    @Column(name = "merchant_name", length = 180)
    private String merchantName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "balance_after_transaction", precision = 19, scale = 2)
    private BigDecimal balanceAfterTransaction;

    @Column(name = "reference_number", length = 120)
    private String referenceNumber;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "is_transfer", nullable = false)
    private boolean transfer;

    @Column(name = "is_ignored", nullable = false)
    private boolean ignored;

    @Column(name = "categorization_confidence", precision = 5, scale = 2)
    private BigDecimal categorizationConfidence;

    @Override
    public String toString() {
        return "Transaction{" +
                "user=" + user +
                ", bankAccount=" + bankAccount +
                ", statementUpload=" + statementUpload +
                ", transactionDate=" + transactionDate +
                ", valueDate=" + valueDate +
                ", description='" + description + '\'' +
                ", rawDescription='" + rawDescription + '\'' +
                ", amount=" + amount +
                ", transactionType=" + transactionType +
                ", paymentMode=" + paymentMode +
                ", merchantName='" + merchantName + '\'' +
                ", category=" + category +
                ", balanceAfterTransaction=" + balanceAfterTransaction +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", recurring=" + recurring +
                ", transfer=" + transfer +
                ", ignored=" + ignored +
                ", categorizationConfidence=" + categorizationConfidence +
                '}';
    }
}
