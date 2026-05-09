package com.upiexpense.intel.rule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRuleRepository extends JpaRepository<MerchantRule, UUID> {

    List<MerchantRule> findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(UUID userId);
}
