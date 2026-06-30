package fu.se2033.pharmacy.controller;

import fu.se2033.pharmacy.dto.flow4.DashboardSummaryResponse;
import fu.se2033.pharmacy.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class Flow4DashboardController {
    private final DashboardService dashboardService;

    public Flow4DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/today")
    public DashboardSummaryResponse today(@RequestParam(required = false) Integer branchId) {
        return dashboardService.getTodaySummary(branchId);
    }
}
