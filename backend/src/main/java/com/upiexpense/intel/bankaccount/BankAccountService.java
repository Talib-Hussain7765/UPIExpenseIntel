package com.upiexpense.intel.bankaccount;

import com.upiexpense.intel.common.error.NotFoundException;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountService {

    private static final Logger log = LoggerFactory.getLogger(BankAccountService.class);
    private static final String DEFAULT_ACCOUNT_TYPE = "SAVINGS";

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository, UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BankAccountResponse create(UUID userId, CreateBankAccountRequest request) {
        log.debug("Creating bank account for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        applyRequest(bankAccount, request.bankName(), request.accountNickname(), request.maskedAccountNumber(), request.accountType());

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
        log.info("Bank account created: userId={}, bankAccountId={}, bankName={}",
                userId,
                savedBankAccount.getId(),
                savedBankAccount.getBankName()
        );
        return BankAccountResponse.from(savedBankAccount);
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> list(UUID userId) {
        List<BankAccountResponse> accounts = bankAccountRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(BankAccountResponse::from)
                .toList();
        log.info("Bank accounts listed: userId={}, count={}", userId, accounts.size());
        return accounts;
    }

    @Transactional(readOnly = true)
    public BankAccountResponse get(UUID userId, UUID bankAccountId) {
        BankAccount bankAccount = findActiveOwnedBankAccount(userId, bankAccountId);
        log.info("Bank account loaded: userId={}, bankAccountId={}", userId, bankAccountId);
        return BankAccountResponse.from(bankAccount);
    }

    @Transactional
    public BankAccountResponse update(UUID userId, UUID bankAccountId, UpdateBankAccountRequest request) {
        BankAccount bankAccount = findActiveOwnedBankAccount(userId, bankAccountId);
        applyRequest(bankAccount, request.bankName(), request.accountNickname(), request.maskedAccountNumber(), request.accountType());

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
        log.info("Bank account updated: userId={}, bankAccountId={}", userId, bankAccountId);
        return BankAccountResponse.from(savedBankAccount);
    }

    @Transactional
    public void deactivate(UUID userId, UUID bankAccountId) {
        BankAccount bankAccount = findActiveOwnedBankAccount(userId, bankAccountId);
        bankAccount.setActive(false);
        bankAccountRepository.save(bankAccount);
        log.info("Bank account deactivated: userId={}, bankAccountId={}", userId, bankAccountId);
    }

    private BankAccount findActiveOwnedBankAccount(UUID userId, UUID bankAccountId) {
        return bankAccountRepository.findByIdAndUserIdAndActiveTrue(bankAccountId, userId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));
    }

    private void applyRequest(
            BankAccount bankAccount,
            String bankName,
            String accountNickname,
            String maskedAccountNumber,
            String accountType
    ) {
        bankAccount.setBankName(bankName.trim());
        bankAccount.setAccountNickname(accountNickname.trim());
        bankAccount.setMaskedAccountNumber(AccountNumberMasker.mask(maskedAccountNumber));
        bankAccount.setAccountType(trimToDefault(accountType, DEFAULT_ACCOUNT_TYPE));
    }

    private String trimToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
