package com.upiexpense.intel.upload;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StatementUploadResponse(
        UUID id,
        UUID bankAccountId,
        String originalFileName,
        String fileType,
        LocalDate statementMonth,
        UploadStatus status,
        int totalRows,
        int parsedRows,
        int skippedRows,
        int duplicateRows,
        int failedRows,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static StatementUploadResponse from(StatementUpload upload) {
        return new StatementUploadResponse(
                upload.getId(),
                upload.getBankAccount().getId(),
                upload.getOriginalFileName(),
                upload.getFileType(),
                upload.getStatementMonth(),
                upload.getStatus(),
                upload.getTotalRows(),
                upload.getParsedRows(),
                upload.getSkippedRows(),
                upload.getDuplicateRows(),
                upload.getFailedRows(),
                upload.getErrorMessage(),
                upload.getCreatedAt(),
                upload.getUpdatedAt()
        );
    }
}
