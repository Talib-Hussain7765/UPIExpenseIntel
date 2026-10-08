package com.upiexpense.intel.funds;

import com.upiexpense.intel.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fund_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(
            name = "total_balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalBalance;

    @Column(name = "reconciled_date", nullable = false)
    private LocalDate reconciledDate;
}
