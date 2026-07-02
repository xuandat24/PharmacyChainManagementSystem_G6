package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.StockTransferCreateRequest;
import fu.se.pharmacy.dto.StockTransferReceiveRequest;
import fu.se.pharmacy.dto.StockTransferSendRequest;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.StockTransfer;
import fu.se.pharmacy.service.StockTransferService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// FIX: trước đây controller này không kiểm tra vai trò — bổ sung requireRole() theo đúng
// flow: BranchManager tạo yêu cầu/gửi/nhận hàng, Admin (hoặc BranchManager chi nhánh gửi)
// duyệt/từ chối. Điều chuyển lớn vẫn được StockTransferServiceImpl tự chuyển sang
// PENDING_ADMIN_APPROVAL theo hạn mức, ở đây chỉ chặn role tối thiểu được phép thao tác.
@RestController
@RequestMapping("/api/stock-transfers")
public class StockTransferController {
    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    public List<StockTransfer> getAll(@RequestParam(required = false) Integer branchId, HttpSession session) {
        AuthInterceptor.requireLogin(session);
        if (branchId != null) {
            return stockTransferService.getByBranch(branchId);
        }
        return stockTransferService.getAll();
    }

    @GetMapping("/{id}")
    public StockTransfer getById(@PathVariable Integer id, HttpSession session) {
        AuthInterceptor.requireLogin(session);
        return stockTransferService.getById(id);
    }

    @PostMapping
    public StockTransfer create(@Valid @RequestBody StockTransferCreateRequest request, HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.createTransfer(request);
    }

    @PostMapping("/{id}/approve")
    public StockTransfer approve(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request,
                                 HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.approveTransfer(id, request);
    }

    @PostMapping("/{id}/reject")
    public StockTransfer reject(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request,
                                HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.rejectTransfer(id, request);
    }

    @PostMapping("/{id}/send")
    public StockTransfer confirmSent(@PathVariable Integer id, @Valid @RequestBody StockTransferSendRequest request,
                                     HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.confirmSent(id, request);
    }

    @PostMapping("/{id}/receive")
    public StockTransfer confirmReceived(@PathVariable Integer id, @Valid @RequestBody StockTransferReceiveRequest request,
                                         HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.confirmReceived(id, request);
    }

    @PostMapping("/{id}/cancel")
    public StockTransfer cancel(@PathVariable Integer id,
                                @RequestParam(required = false) String reason,
                                HttpSession session) {
        // FIX: userId trước đây lấy từ @RequestParam (client tự khai, dễ giả mạo) — nay lấy từ session.
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        return stockTransferService.cancelTransfer(id, user.getUserId(), reason);
    }
}
