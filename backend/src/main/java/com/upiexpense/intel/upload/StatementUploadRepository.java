package com.upiexpense.intel.upload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementUploadRepository extends JpaRepository<StatementUpload, UUID> {

    List<StatementUpload> findByUserIdAndStatementMonthOrderByCreatedAtDesc(UUID userId, LocalDate statementMonth);
}
