package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.GoodsReceiptForm;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.service.GoodsReceiptService;
import fu.se.pharmacy.service.PurchaseRequestService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 2 - Nhận hàng):
 * "Manager chọn Purchase Request đã duyệt → Tạo Goods Receipt."
 * "Trường hợp có chênh lệch → PENDING_ADMIN_APPROVAL → Admin duyệt lại."
 * → Tạo/lưu phiếu nhận hàng: chỉ BranchManager.
 * → Duyệt/từ chối chênh lệch (admin/*): chỉ Admin.
 * → Xem danh sách: mọi role đã đăng nhập (Admin xem toàn hệ thống, còn lại theo chi nhánh).
 *
 * FIX: chuẩn hóa dùng AuthInterceptor.requireRole()/requireLogin() — trước đây check
 * thủ công bằng if/else và redirect âm thầm về list khi sai quyền, không nhất quán
 * với trang "Bạn không có quyền hạn truy cập" của các luồng khác trong hệ thống.
 */
@Controller
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {

    @Autowired private GoodsReceiptService grService;
    @Autowired private PurchaseRequestService prService;

    @GetMapping
    public String listReceipts(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        List<GoodsReceipt> receipts;
        if (user.isAdmin()) {
            receipts = grService.getAllReceipts();
        } else {
            receipts = grService.getReceiptsByBranch(user.getBranchId());
        }

        model.addAttribute("receipts", receipts);
        model.addAttribute("user", user);
        return "goods-receipts/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("requestId") Integer requestId,
                                 HttpSession session, Model model) {
        // FIX: chỉ BranchManager được nhận hàng
        AuthInterceptor.requireRole(session, "BranchManager");

        PurchaseRequest pr = prService.getRequestById(requestId);
        if (!"APPROVED".equals(pr.getStatus()) && !"PARTIALLY_APPROVED".equals(pr.getStatus())) {
            return "redirect:/purchase-requests";
        }

        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseRequestId(requestId);

        for (PurchaseRequestDetail d : pr.getDetails()) {
            Integer approved = d.getApprovedQuantity();
            if (approved != null && approved > 0) {
                GoodsReceiptForm.Item item = new GoodsReceiptForm.Item();
                item.setMedicineId(d.getMedicineId());
                item.setQuantityReceived(approved);
                item.setPurchasePrice(d.getExpectedUnitPrice());
                item.setExpiryDate(LocalDate.now().plusYears(2));
                form.getItems().add(item);
            }
        }

        model.addAttribute("form", form);
        model.addAttribute("pr", pr);
        return "goods-receipts/create";
    }

    @PostMapping("/save")
    public String saveReceipt(@Valid @ModelAttribute("form") GoodsReceiptForm form,
                              BindingResult result, HttpSession session, Model model) {
        // FIX: chỉ BranchManager được lưu phiếu nhận hàng
        AppUser user = AuthInterceptor.requireRole(session, "BranchManager");

        PurchaseRequest pr = prService.getRequestById(form.getPurchaseRequestId());

        if (result.hasErrors()) {
            model.addAttribute("pr", pr);
            return "goods-receipts/create";
        }

        GoodsReceipt gr = new GoodsReceipt();
        gr.setPurchaseRequestId(pr.getPurchaseRequestId());
        gr.setBranchId(user.getBranchId());
        gr.setSupplierId(pr.getSupplierId());
        gr.setReceivedBy(user.getUserId());
        gr.setStatus("DRAFT");
        gr.setTotalActualAmount(0);
        gr.setHasVariance(false);

        List<GoodsReceiptDetail> details = new ArrayList<>();
        for (GoodsReceiptForm.Item item : form.getItems()) {
            GoodsReceiptDetail detail = new GoodsReceiptDetail();
            detail.setMedicineId(item.getMedicineId());
            detail.setBatchNumber(item.getBatchNumber() != null ? item.getBatchNumber()
                    : "LOT-" + System.currentTimeMillis());
            detail.setExpiryDate(item.getExpiryDate());
            detail.setReceivedQuantity(item.getQuantityReceived());
            detail.setAcceptedQuantity(item.getQuantityReceived());
            detail.setRejectedQuantity(0);
            detail.setOrderedQuantity(item.getQuantityReceived());
            detail.setActualUnitPrice(item.getPurchasePrice() != null ? item.getPurchasePrice() : 0);
            detail.setInspectionResult("PASS");
            details.add(detail);
        }
        gr.setDetails(details);

        grService.saveReceipt(gr);
        grService.submitAndCheckVariance(gr.getReceiptId());

        return "redirect:/goods-receipts";
    }

    @GetMapping("/admin/pending")
    public String showPendingReceipts(HttpSession session, Model model) {
        // FIX: chỉ Admin duyệt chênh lệch nhận hàng
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("receipts", grService.getPendingApprovalReceipts());
        return "goods-receipts/pending";
    }

    @GetMapping("/admin/approve-variance/{id}")
    public String showApproveVarianceForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");

        GoodsReceipt receipt = grService.getReceiptById(id);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) return "redirect:/goods-receipts";

        model.addAttribute("gr", receipt);
        return "goods-receipts/approve-variance";
    }

    @PostMapping("/admin/approve-variance/{id}")
    public String approveVariance(@PathVariable Integer id,
                                  @RequestParam(required = false) String adminNotes,
                                  HttpSession session) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin");
        grService.approveVariance(id, user.getUserId(), adminNotes);
        return "redirect:/goods-receipts";
    }

    @PostMapping("/admin/reject-variance/{id}")
    public String rejectVariance(@PathVariable Integer id,
                                 @RequestParam(required = false) String adminNotes,
                                 HttpSession session) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin");
        grService.rejectReceipt(id, user.getUserId(), adminNotes);
        return "redirect:/goods-receipts";
    }
}
