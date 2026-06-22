package com.upiexpense.intel.rule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRuleRepository extends JpaRepository<MerchantRule, UUID> {

    List<MerchantRule> findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(UUID userId);

    List<MerchantRule> findByUserIdOrderByPriorityAscCreatedAtAsc(UUID userId);

    Optional<MerchantRule> findByIdAndUserId(UUID id, UUID userId);
}
