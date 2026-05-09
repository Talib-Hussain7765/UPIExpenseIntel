package com.upiexpense.intel.report;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, UUID> {

    Optional<MonthlyReport> findByUserIdAndReportMonth(UUID userId, LocalDate reportMonth);
}
