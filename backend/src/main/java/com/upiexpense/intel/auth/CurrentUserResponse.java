package com.upiexpense.intel.auth;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String fullName,
        String timezone,
        String currency,
        boolean monthlyReportEnabled,
        int monthlyReportDay
) {
}
