package com.upiexpense.intel.user;

import com.upiexpense.intel.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Kolkata";

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "monthly_report_enabled", nullable = false)
    private boolean monthlyReportEnabled;

    @Column(name = "monthly_report_day", nullable = false)
    private int monthlyReportDay = 1;
}
