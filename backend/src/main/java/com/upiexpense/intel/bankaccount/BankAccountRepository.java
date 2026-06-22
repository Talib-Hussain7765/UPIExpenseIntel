package com.upiexpense.intel.bankaccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    Optional<BankAccount> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);
}
