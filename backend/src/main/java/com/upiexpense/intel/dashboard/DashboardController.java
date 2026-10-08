package com.upiexpense.intel.dashboard;

import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    DashboardSummaryResponse summaryRange(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String dateBasis,@RequestParam(required = false) String month, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate
    ) {

        LocalDate startDateParsed = null;
        LocalDate endDateParsed = null;
        if(dateBasis.equals("MONTH")) {
            YearMonth yearMonth = parseMonth(month);
            startDateParsed = yearMonth.atDay(1);
            endDateParsed = yearMonth.atEndOfMonth();
        }
        else {
            startDateParsed = LocalDate.parse(startDate);
            endDateParsed = LocalDate.parse(endDate);
        }
        return dashboardService.summary(currentUser.id(), startDateParsed, endDateParsed);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new BadRequestException("month must use yyyy-MM format");
        }
    }
}
