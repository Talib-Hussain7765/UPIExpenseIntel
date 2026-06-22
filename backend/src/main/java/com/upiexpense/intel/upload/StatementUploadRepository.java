package com.upiexpense.intel.upload;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementUploadRepository extends JpaRepository<StatementUpload, UUID> {

    List<StatementUpload> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<StatementUpload> findByUserIdAndStatementMonthOrderByCreatedAtDesc(UUID userId, LocalDate statementMonth);

    Optional<StatementUpload> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndBankAccountIdAndStatementMonthAndFileSha256(
            UUID userId,
            UUID bankAccountId,
            LocalDate statementMonth,
            String fileSha256
    );
}
