package com.upiexpense.intel.upload;

import com.upiexpense.intel.common.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class StatementUploadController {

    private final StatementUploadService statementUploadService;

    public StatementUploadController(StatementUploadService statementUploadService) {
        this.statementUploadService = statementUploadService;
    }

    @PostMapping("/statements")
    @ResponseStatus(HttpStatus.CREATED)
    StatementUploadResponse uploadStatement(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam UUID bankAccountId,
            @RequestParam String statementMonth,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String pdfPassword
    ) {
        return statementUploadService.uploadStatement(
                currentUser.id(),
                bankAccountId,
                statementMonth,
                file,
                pdfPassword
        );
    }

    @GetMapping
    List<StatementUploadResponse> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String statementMonth
    ) {
        return statementUploadService.list(currentUser.id(), statementMonth);
    }

    @GetMapping("/{id}")
    StatementUploadResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return statementUploadService.get(currentUser.id(), id);
    }

    @DeleteMapping("/{statement_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID statement_id) {
        statementUploadService.delete(currentUser.id(), statement_id);
    }
}
