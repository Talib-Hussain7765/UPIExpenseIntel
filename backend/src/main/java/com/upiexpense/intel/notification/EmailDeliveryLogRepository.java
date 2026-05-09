package com.upiexpense.intel.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, UUID> {

    List<EmailDeliveryLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
