package com.upiexpense.intel.common.error;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, error, message, path, Map.of());
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, error, message, path, fieldErrors);
    }
}
