package com.upiexpense.intel.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByUserIdAndBankAccountIdAndTransactionDateBetween(
            UUID userId,
            UUID bankAccountId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserIdAndStatementUploadId(UUID userId, UUID uploadId);
}
