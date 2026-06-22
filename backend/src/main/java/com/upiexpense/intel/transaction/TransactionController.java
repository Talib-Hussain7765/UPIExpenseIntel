package com.upiexpense.intel.transaction;

import com.upiexpense.intel.common.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    List<TransactionResponse> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) PaymentMode paymentMode,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) Boolean ignored,
            @RequestParam(required = false) Boolean transfer
    ) {
        return transactionService.list(
                currentUser.id(),
                month,
                transactionType,
                paymentMode,
                categoryId,
                merchant,
                ignored,
                transfer
        );
    }

    @GetMapping("/{id}")
    TransactionResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return transactionService.get(currentUser.id(), id);
    }

    @PatchMapping("/{id}")
    TransactionResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @RequestBody UpdateTransactionRequest request
    ) {
        return transactionService.update(currentUser.id(), id, request);
    }

    @PostMapping("/categorize")
    CategorizationResponse categorize(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String month,
            @RequestParam(defaultValue = "false") boolean overwrite
    ) {
        return transactionService.categorizeMonth(currentUser.id(), month, overwrite);
    }
}
