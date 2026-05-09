package com.upiexpense.intel.report;

import com.upiexpense.intel.common.domain.BaseEntity;
import com.upiexpense.intel.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "monthly_reports")
public class MonthlyReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "total_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "savings_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal savingsAmount = BigDecimal.ZERO;

    @Column(name = "savings_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal savingsRate = BigDecimal.ZERO;

    @Column(name = "report_html_path", length = 500)
    private String reportHtmlPath;

    @Column(name = "report_pdf_path", length = 500)
    private String reportPdfPath;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
}
