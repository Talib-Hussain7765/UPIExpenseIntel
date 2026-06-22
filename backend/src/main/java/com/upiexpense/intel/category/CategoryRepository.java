package com.upiexpense.intel.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(UUID userId);

    Optional<Category> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(UUID userId, String name);
}
