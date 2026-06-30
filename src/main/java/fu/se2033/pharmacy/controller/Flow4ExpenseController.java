package fu.se2033.pharmacy.controller;

import fu.se2033.pharmacy.dto.flow4.ApprovalRequest;
import fu.se2033.pharmacy.dto.flow4.ExpenseVoucherCreateRequest;
import fu.se2033.pharmacy.entity.ExpenseVoucher;
import fu.se2033.pharmacy.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class Flow4ExpenseController {
    private final ExpenseService expenseService;

    public Flow4ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseVoucher> getAll(@RequestParam(required = false) Integer branchId) {
        if (branchId != null) {
            return expenseService.getByBranch(branchId);
        }
        return expenseService.getAll();
    }

    @GetMapping("/{id}")
    public ExpenseVoucher getById(@PathVariable Integer id) {
        return expenseService.getById(id);
    }

    @PostMapping
    public ExpenseVoucher create(@Valid @RequestBody ExpenseVoucherCreateRequest request) {
        return expenseService.createExpense(request);
    }

    @PostMapping("/{id}/submit")
    public ExpenseVoucher submit(@PathVariable Integer id) {
        return expenseService.submitExpense(id);
    }

    @PostMapping("/{id}/approve")
    public ExpenseVoucher approve(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request) {
        return expenseService.approveExpense(id, request);
    }

    @PostMapping("/{id}/reject")
    public ExpenseVoucher reject(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request) {
        return expenseService.rejectExpense(id, request);
    }

    @PostMapping("/{id}/mark-paid")
    public ExpenseVoucher markPaid(@PathVariable Integer id, @RequestParam Integer userId) {
        return expenseService.markPaid(id, userId);
    }
}
