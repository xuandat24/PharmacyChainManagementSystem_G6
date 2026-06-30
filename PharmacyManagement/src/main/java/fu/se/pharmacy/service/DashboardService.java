package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getTodaySummary(Integer branchId);
}
