package com.upiexpense.intel.bankaccount;

import com.upiexpense.intel.common.domain.BaseEntity;
import com.upiexpense.intel.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_accounts")
public class BankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "account_nickname", nullable = false, length = 120)
    private String accountNickname;

    @Column(name = "masked_account_number", length = 40)
    private String maskedAccountNumber;

    @Column(name = "account_type", nullable = false, length = 40)
    private String accountType = "SAVINGS";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
