package com.upiexpense.intel.funds;

import com.upiexpense.intel.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    @GetMapping("/summary")
    FundSummaryResponse summaryRange(@AuthenticationPrincipal CurrentUser currentUser) {
        return fundService.getFundSummary(currentUser.id());
    }

    @GetMapping("/transactions")
    FundTransactionHistoryResponse transactionHistory(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) UUID fundId,
            @RequestParam(defaultValue = "LAST_MONTH") FundActivityPeriod period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return fundService.getTransactionHistory(
                currentUser.id(),
                fundId,
                period,
                startDate,
                endDate
        );
    }

    @PostMapping
    FundResponse createFund(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateFundRequest request
    ) {
        return fundService.createFund(currentUser.id(), request);
    }

    @PostMapping("/{fundId}/transactions")
    ResponseEntity<Void> updateFund(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID fundId,
            @Valid @RequestBody UpdateFundRequest request
    ) {

        fundService.updateFund(currentUser.id(), fundId, request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reconcile")
    ResponseEntity<Void> reconcileFund(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ReconcileFundRequest request
    ) {

        fundService.reconcile(currentUser.id(), request);

        return ResponseEntity.noContent().build();
    }


}
