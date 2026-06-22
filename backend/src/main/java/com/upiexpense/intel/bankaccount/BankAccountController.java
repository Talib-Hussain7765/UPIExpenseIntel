package com.upiexpense.intel.bankaccount;

import com.upiexpense.intel.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    List<BankAccountResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return bankAccountService.list(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BankAccountResponse create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateBankAccountRequest request
    ) {
        return bankAccountService.create(currentUser.id(), request);
    }

    @GetMapping("/{id}")
    BankAccountResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return bankAccountService.get(currentUser.id(), id);
    }

    @PutMapping("/{id}")
    BankAccountResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBankAccountRequest request
    ) {
        return bankAccountService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        bankAccountService.deactivate(currentUser.id(), id);
    }
}
