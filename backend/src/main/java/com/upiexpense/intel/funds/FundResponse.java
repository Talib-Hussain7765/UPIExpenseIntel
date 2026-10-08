package com.upiexpense.intel.funds;

import java.math.BigDecimal;
import java.util.UUID;

public record FundResponse(
        UUID id,
        String name,
        BigDecimal balance
) {
}
