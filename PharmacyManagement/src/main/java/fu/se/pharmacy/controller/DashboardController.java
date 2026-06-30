package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.DashboardSummaryResponse;
import fu.se.pharmacy.service.DashboardService;
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

    @GetMapping("/today")
    public DashboardSummaryResponse today(@RequestParam(required = false) Integer branchId) {
        return dashboardService.getTodaySummary(branchId);
    }
}
