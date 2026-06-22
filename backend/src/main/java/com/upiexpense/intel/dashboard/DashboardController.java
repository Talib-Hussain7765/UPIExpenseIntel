package com.upiexpense.intel.dashboard;

import com.upiexpense.intel.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    DashboardSummaryResponse summary(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String month
    ) {
        return dashboardService.summary(currentUser.id(), month);
    }
}
