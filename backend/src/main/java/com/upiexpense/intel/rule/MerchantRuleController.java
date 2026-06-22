package com.upiexpense.intel.rule;

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
@RequestMapping("/api/merchant-rules")
public class MerchantRuleController {

    private final MerchantRuleService merchantRuleService;

    public MerchantRuleController(MerchantRuleService merchantRuleService) {
        this.merchantRuleService = merchantRuleService;
    }

    @GetMapping
    List<MerchantRuleResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return merchantRuleService.list(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MerchantRuleResponse create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateMerchantRuleRequest request
    ) {
        return merchantRuleService.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    MerchantRuleResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantRuleRequest request
    ) {
        return merchantRuleService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        merchantRuleService.deactivate(currentUser.id(), id);
    }
}
