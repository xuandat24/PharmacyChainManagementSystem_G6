package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.ExpenseVoucherCreateRequest;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.ExpenseVoucher;
import fu.se.pharmacy.service.ExpenseService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// FIX: trước đây controller này không kiểm tra vai trò gì cả — bất kỳ user nào
// (kể cả Pharmacist) đều gọi được API tạo/duyệt/từ chối/thanh toán phiếu chi.
// Bổ sung requireRole() đúng theo flow: BranchManager tạo/gửi, Admin duyệt/từ chối/thanh toán.
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseVoucher> getAll(@RequestParam(required = false) Integer branchId, HttpSession session) {
        AuthInterceptor.requireLogin(session);
        if (branchId != null) {
            return expenseService.getByBranch(branchId);
        }
        return expenseService.getAll();
    }

    @GetMapping("/{id}")
    public ExpenseVoucher getById(@PathVariable Integer id, HttpSession session) {
        AuthInterceptor.requireLogin(session);
        return expenseService.getById(id);
    }

    @PostMapping
    public ExpenseVoucher create(@Valid @RequestBody ExpenseVoucherCreateRequest request, HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return expenseService.createExpense(request);
    }

    @PostMapping("/{id}/submit")
    public ExpenseVoucher submit(@PathVariable Integer id, HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return expenseService.submitExpense(id);
    }

    @PostMapping("/{id}/approve")
    public ExpenseVoucher approve(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request,
                                  HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin");
        return expenseService.approveExpense(id, request);
    }

    @PostMapping("/{id}/reject")
    public ExpenseVoucher reject(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request,
                                 HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin");
        return expenseService.rejectExpense(id, request);
    }

    @PostMapping("/{id}/mark-paid")
    public ExpenseVoucher markPaid(@PathVariable Integer id, HttpSession session) {
        // FIX: trước đây userId lấy từ @RequestParam (client tự khai) — bất kỳ ai cũng
        // giả mạo được người xác nhận thanh toán. Nay lấy đúng từ session đăng nhập.
        AppUser user = AuthInterceptor.requireRole(session, "Admin");
        return expenseService.markPaid(id, user.getUserId());
    }
}
