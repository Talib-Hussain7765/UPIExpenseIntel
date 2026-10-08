package com.upiexpense.intel.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Transaction> findByUserIdAndBankAccountIdAndTransactionDateBetween(
            UUID userId,
            UUID bankAccountId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Optional<Transaction> findFirstByUserIdAndStatementUploadIdOrderByTransactionDateAsc(
            UUID userId,
            UUID uploadId
    );

    void deleteByUserIdAndStatementUploadId(UUID userId, UUID uploadId);
}
