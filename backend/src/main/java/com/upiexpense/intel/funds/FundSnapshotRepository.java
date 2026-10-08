package com.upiexpense.intel.funds;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundSnapshotRepository extends JpaRepository<FundSnapshot, UUID> {

    Optional<FundSnapshot>
    findFirstByUserIdOrderByReconciledDateDescCreatedAtDesc(
            UUID userId
    );

}
