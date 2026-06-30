package fu.se.pharmacy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * FIX: view name truoc day tro toi "views/flow4/..." nhung khi merge tay,
 * templates da duoc flatten ra thang templates/transfers/, templates/expenses/,
 * templates/reports/, templates/periods/, templates/notifications/,
 * templates/audit-logs/, templates/dashboard.html (khong con thu muc views/flow4/
 * nao ca). Sua lai view name cho dung voi vi tri thuc te cua file .html.
 */
@Controller
@RequestMapping("/flow4")
public class PageController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/transfers")
    public String transfers() {
        return "transfers/list";
    }

    @GetMapping("/transfers/create")
    public String createTransfer() {
        return "transfers/form";
    }

    @GetMapping("/transfers/{id}")
    public String transferDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("transferId", id);
        return "transfers/detail";
    }

    @GetMapping("/expenses")
    public String expenses() {
        return "expenses/list";
    }

    @GetMapping("/expenses/create")
    public String createExpense() {
        return "expenses/form";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports/index";
    }

    @GetMapping("/periods")
    public String periods() {
        return "periods/index";
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return "audit-logs/list";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications/list";
    }
}
