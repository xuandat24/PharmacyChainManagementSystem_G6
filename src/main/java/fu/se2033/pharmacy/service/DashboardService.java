package fu.se2033.pharmacy.service;

import fu.se2033.pharmacy.dto.flow4.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getTodaySummary(Integer branchId);
}
