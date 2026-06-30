package fu.se.pharmacy.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {
    List<Map<String, Object>> revenueReport(LocalDate fromDate, LocalDate toDate, Integer branchId);
    List<Map<String, Object>> expenseReport(LocalDate fromDate, LocalDate toDate, Integer branchId);
    List<Map<String, Object>> inventoryMovementReport(LocalDate fromDate, LocalDate toDate, Integer branchId);
}
