package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FIX: Báo cáo doanh thu/chi phí/tồn kho chỉ dành cho Admin và BranchManager.
 * Pharmacist không được xem báo cáo tổng hợp (theo Screen Authorization).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    public List<Map<String, Object>> revenue(
            HttpSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer branchId) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return reportService.revenueReport(fromDate, toDate, branchId);
    }

    @GetMapping("/expenses")
    public List<Map<String, Object>> expenses(
            HttpSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer branchId) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return reportService.expenseReport(fromDate, toDate, branchId);
    }

    @GetMapping("/inventory-movements")
    public List<Map<String, Object>> inventoryMovements(
            HttpSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer branchId) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return reportService.inventoryMovementReport(fromDate, toDate, branchId);
    }
}
