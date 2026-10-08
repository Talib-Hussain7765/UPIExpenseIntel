package com.upiexpense.intel.funds;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FundTransactionRepository extends JpaRepository<FundTransaction, UUID> {
    List<FundTransaction>
    findByUserIdAndFundIdOrderByCreatedAtDesc(
            UUID userId,
            UUID fundId
    );

    List<FundTransaction>
    findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<FundTransaction>
    findByUserIdAndFundIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            UUID userId,
            UUID fundId,
            LocalDate startDate,
            LocalDate endDate
    );
}
