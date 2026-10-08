package com.upiexpense.intel.funds;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundRepository extends JpaRepository<Fund, UUID> {
    List<Fund> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Fund> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
