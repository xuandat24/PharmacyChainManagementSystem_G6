package fu.se2033.pharmacy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/flow4")
public class Flow4PageController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "views/flow4/dashboard";
    }

    @GetMapping("/transfers")
    public String transfers() {
        return "views/flow4/transfers/list";
    }

    @GetMapping("/transfers/create")
    public String createTransfer() {
        return "views/flow4/transfers/form";
    }

    @GetMapping("/transfers/{id}")
    public String transferDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("transferId", id);
        return "views/flow4/transfers/detail";
    }

    @GetMapping("/expenses")
    public String expenses() {
        return "views/flow4/expenses/list";
    }

    @GetMapping("/expenses/create")
    public String createExpense() {
        return "views/flow4/expenses/form";
    }

    @GetMapping("/reports")
    public String reports() {
        return "views/flow4/reports/index";
    }

    @GetMapping("/periods")
    public String periods() {
        return "views/flow4/periods/index";
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return "views/flow4/audit-logs/list";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "views/flow4/notifications/list";
    }
}
