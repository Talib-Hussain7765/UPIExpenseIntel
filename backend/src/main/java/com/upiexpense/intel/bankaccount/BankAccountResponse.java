package com.upiexpense.intel.bankaccount;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        String bankName,
        String accountNickname,
        String maskedAccountNumber,
        String accountType,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static BankAccountResponse from(BankAccount bankAccount) {
        return new BankAccountResponse(
                bankAccount.getId(),
                bankAccount.getBankName(),
                bankAccount.getAccountNickname(),
                AccountNumberMasker.mask(bankAccount.getMaskedAccountNumber()),
                bankAccount.getAccountType(),
                bankAccount.isActive(),
                bankAccount.getCreatedAt(),
                bankAccount.getUpdatedAt()
        );
    }
}
