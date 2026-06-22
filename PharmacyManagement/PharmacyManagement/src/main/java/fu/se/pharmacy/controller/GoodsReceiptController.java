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

    @Autowired
    private GoodsReceiptService grService;

    @Autowired
    private PurchaseRequestService prService;

    @GetMapping
    public String listReceipts(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<GoodsReceipt> receipts;
        if ("Admin".equals(user.getRole())) {
            receipts = grService.getReceiptsByBranch(user.getBranch().getBranchId()); // Hoặc get tất cả
            // Tạm lấy tất cả
            receipts = grService.getReceiptsByBranch(user.getBranch().getBranchId());
        } else {
            receipts = grService.getReceiptsByBranch(user.getBranch().getBranchId());
        }

        model.addAttribute("receipts", receipts);
        model.addAttribute("user", user);
        return "goods-receipts/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("requestId") Integer requestId, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        PurchaseRequest pr = prService.getRequestById(requestId);
        if (!"APPROVED".equals(pr.getStatus()) && !"PARTIALLY_APPROVED".equals(pr.getStatus())) {
            return "redirect:/purchase-requests";
        }

        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseRequestId(requestId);

        for (PurchaseRequestDetail d : pr.getDetails()) {
            if (d.getQuantityApproved() > 0) {
                GoodsReceiptForm.Item item = new GoodsReceiptForm.Item();
                item.setMedicineId(d.getMedicine().getMedicineId());
                item.setQuantityReceived(d.getQuantityApproved()); // Mặc định khớp
                item.setPurchasePrice(d.getEstimatedPrice());      // Mặc định khớp
                item.setExpiryDate(LocalDate.now().plusYears(2));  // Mặc định 2 năm
                form.getItems().add(item);
            }
        }

        model.addAttribute("form", form);
        model.addAttribute("pr", pr);
        return "goods-receipts/create";
    }

    @PostMapping("/save")
    public String saveReceipt(@Valid @ModelAttribute("form") GoodsReceiptForm form, BindingResult result, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        PurchaseRequest pr = prService.getRequestById(form.getPurchaseRequestId());

        if (result.hasErrors()) {
            model.addAttribute("pr", pr);
            return "goods-receipts/create";
        }

        GoodsReceipt gr = new GoodsReceipt();
        gr.setPurchaseRequest(pr);
        gr.setBranch(user.getBranch());
        gr.setReceivedBy(user);
        gr.setStatus("DRAFT");

        List<GoodsReceiptDetail> details = new ArrayList<>();
        for (GoodsReceiptForm.Item item : form.getItems()) {
            GoodsReceiptDetail detail = new GoodsReceiptDetail();
            detail.setGoodsReceipt(gr);
            detail.setMedicine(new Medicine());
            detail.getMedicine().setMedicineId(item.getMedicineId());
            detail.setBatchNumber(item.getBatchNumber());
            detail.setExpiryDate(item.getExpiryDate());
            detail.setManufactureDate(item.getManufactureDate());
            detail.setQuantityReceived(item.getQuantityReceived());
            detail.setPurchasePrice(item.getPurchasePrice());
            details.add(detail);
        }
        gr.setDetails(details);

        grService.saveReceipt(gr);
        grService.submitAndCheckVariance(gr.getReceiptId());

        return "redirect:/goods-receipts";
    }

    @GetMapping("/admin/pending")
    public String showPendingReceipts(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        model.addAttribute("receipts", grService.getPendingApprovalReceipts());
        return "goods-receipts/pending";
    }

    @GetMapping("/admin/approve-variance/{id}")
    public String showApproveVarianceForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        GoodsReceipt receipt = grService.getReceiptById(id);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) {
            return "redirect:/goods-receipts";
        }

        model.addAttribute("gr", receipt);
        return "goods-receipts/approve-variance";
    }

    @PostMapping("/admin/approve-variance/{id}")
    public String approveVariance(@PathVariable("id") Integer id,
                                  @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                  HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        grService.approveVariance(id, user.getEmployeeId(), adminNotes);
        return "redirect:/goods-receipts";
    }

    @PostMapping("/admin/reject-variance/{id}")
    public String rejectVariance(@PathVariable("id") Integer id,
                                 @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                 HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/goods-receipts";
        }

        grService.rejectReceipt(id, user.getEmployeeId(), adminNotes);
        return "redirect:/goods-receipts";
    }
}
