package fu.se.pharmacy.controller;

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

@Controller
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {

    @Autowired private GoodsReceiptService grService;
    @Autowired private PurchaseRequestService prService;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    @GetMapping
    public String listReceipts(HttpSession session, Model model) {
        AppUser user = getUser(session);
        if (user == null) return "redirect:/login";

        // FIX 1: AppUser không có getBranch() → dùng branchId trực tiếp
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
        AppUser user = getUser(session);
        // FIX 2: getRole() nay đã hoạt động thông qua helper method
        if (user == null || !user.isBranchManager()) {
            return "redirect:/goods-receipts";
        }

        PurchaseRequest pr = prService.getRequestById(requestId);
        if (!"APPROVED".equals(pr.getStatus()) && !"PARTIALLY_APPROVED".equals(pr.getStatus())) {
            return "redirect:/purchase-requests";
        }

        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseRequestId(requestId);

        // FIX 3: PurchaseRequestDetail dùng approvedQuantity, expectedUnitPrice thay vì QuantityApproved/EstimatedPrice
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
        AppUser user = getUser(session);
        if (user == null || !user.isBranchManager()) {
            return "redirect:/goods-receipts";
        }

        PurchaseRequest pr = prService.getRequestById(form.getPurchaseRequestId());

        if (result.hasErrors()) {
            model.addAttribute("pr", pr);
            return "goods-receipts/create";
        }

        // FIX 4: GoodsReceipt entity dùng integer FK (branchId, receivedBy) không phải @ManyToOne
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
        AppUser user = getUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/goods-receipts";
        model.addAttribute("receipts", grService.getPendingApprovalReceipts());
        return "goods-receipts/pending";
    }

    @GetMapping("/admin/approve-variance/{id}")
    public String showApproveVarianceForm(@PathVariable Integer id, HttpSession session, Model model) {
        AppUser user = getUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/goods-receipts";

        GoodsReceipt receipt = grService.getReceiptById(id);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) return "redirect:/goods-receipts";

        model.addAttribute("gr", receipt);
        return "goods-receipts/approve-variance";
    }

    @PostMapping("/admin/approve-variance/{id}")
    public String approveVariance(@PathVariable Integer id,
                                  @RequestParam(required = false) String adminNotes,
                                  HttpSession session) {
        AppUser user = getUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/goods-receipts";
        grService.approveVariance(id, user.getUserId(), adminNotes);
        return "redirect:/goods-receipts";
    }

    @PostMapping("/admin/reject-variance/{id}")
    public String rejectVariance(@PathVariable Integer id,
                                 @RequestParam(required = false) String adminNotes,
                                 HttpSession session) {
        AppUser user = getUser(session);
        if (user == null || !user.isAdmin()) return "redirect:/goods-receipts";
        grService.rejectReceipt(id, user.getUserId(), adminNotes);
        return "redirect:/goods-receipts";
    }
}