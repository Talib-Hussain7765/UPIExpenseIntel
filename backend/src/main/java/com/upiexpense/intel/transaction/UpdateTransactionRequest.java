package com.upiexpense.intel.transaction;

import java.util.UUID;

public record UpdateTransactionRequest(
        UUID categoryId,
        Boolean transfer,
        Boolean ignored
) {
}
