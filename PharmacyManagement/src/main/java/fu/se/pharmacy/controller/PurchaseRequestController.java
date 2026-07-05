package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.PurchaseRequestForm;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.MedicineRepository;
import fu.se.pharmacy.service.PurchaseRequestService;
import fu.se.pharmacy.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/purchase-requests")
public class PurchaseRequestController {

    @Autowired private PurchaseRequestService prService;
    @Autowired private SupplierService supplierService;
    @Autowired private MedicineRepository medicineRepository;

    @GetMapping
    public String listRequests(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        List<PurchaseRequest> requests;
        if (user.isAdmin()) {
            requests = prService.getAllRequests();
        } else {
            requests = prService.getRequestsByBranch(user.getBranchId());
        }
        model.addAttribute("requests", requests);
        model.addAttribute("user", user);
        return "purchase-requests/list";
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        // Chỉ BranchManager tạo được → Pharmacist/Admin vào đây nhận trang 403
        AuthInterceptor.requireRole(session, "BranchManager");
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("medicines", medicineRepository.findAll());
        model.addAttribute("form", new PurchaseRequestForm());
        return "purchase-requests/create";
    }

    @PostMapping("/save")
    public String saveRequest(@Valid @ModelAttribute("form") PurchaseRequestForm form,
                              BindingResult result, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "BranchManager");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");

        if (result.hasErrors() || form.getItems().isEmpty()) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("medicines", medicineRepository.findAll());
            if (form.getItems().isEmpty())
                model.addAttribute("error", "Vui lòng thêm ít nhất một mặt hàng yêu cầu nhập.");
            return "purchase-requests/create";
        }

        PurchaseRequest pr = new PurchaseRequest();
        pr.setBranchId(user.getBranchId());
        pr.setRequestedBy(user.getUserId());
        pr.setSupplierId(form.getSupplierId());
        pr.setStatus("DRAFT");
        pr.setTotalEstimatedAmount(0);

        List<PurchaseRequestDetail> details = new ArrayList<>();
        for (PurchaseRequestForm.Item item : form.getItems()) {
            PurchaseRequestDetail detail = new PurchaseRequestDetail();
            detail.setMedicineId(item.getMedicineId());
            detail.setRequestedQuantity(item.getQuantityRequested());
            detail.setApprovedQuantity(0);
            detail.setExpectedUnitPrice(item.getEstimatedPrice() != null ? item.getEstimatedPrice() : 0);
            details.add(detail);
        }
        pr.setDetails(details);
        prService.saveRequest(pr);
        return "redirect:/purchase-requests";
    }

    @GetMapping("/submit/{id}")
    public String submitRequest(@PathVariable Integer id,
                                HttpSession session,
                                RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "BranchManager");
        try {
            prService.submitRequest(id);
            ra.addFlashAttribute("success", "Đã gửi yêu cầu đi phê duyệt");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-requests";
    }

    @GetMapping("/admin/approve/{id}")
    public String showApproveForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        PurchaseRequest pr = prService.getRequestById(id);
        if (!"SUBMITTED".equals(pr.getStatus())) return "redirect:/purchase-requests";
        model.addAttribute("pr", pr);
        return "purchase-requests/approve";
    }

    @PostMapping("/admin/approve/{id}")
    public String approveRequest(@PathVariable Integer id,
                                 @RequestParam(required = false) String adminNotes,
                                 @RequestParam("detailIds") List<Integer> detailIds,
                                 @RequestParam("approvedQuantities") List<Integer> approvedQuantities,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");

        List<PurchaseRequestDetail> approvedDetails = new ArrayList<>();
        for (int i = 0; i < detailIds.size(); i++) {
            PurchaseRequestDetail d = new PurchaseRequestDetail();
            d.setPurchaseRequestDetailId(detailIds.get(i));
            d.setApprovedQuantity(approvedQuantities.get(i));
            approvedDetails.add(d);
        }
        try {
            prService.approveRequest(id, user.getUserId(), adminNotes, approvedDetails);
            ra.addFlashAttribute("success", "Đã phê duyệt yêu cầu nhập hàng");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-requests";
    }

    @PostMapping("/admin/reject/{id}")
    public String rejectRequest(@PathVariable Integer id,
                                @RequestParam(required = false) String adminNotes,
                                HttpSession session,
                                RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        try {
            prService.rejectRequest(id, user.getUserId(), adminNotes);
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu nhập hàng");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-requests";
    }

    @GetMapping("/cancel/{id}")
    public String cancelRequest(@PathVariable Integer id,
                                HttpSession session,
                                RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "BranchManager");
        try {
            prService.cancelRequest(id);
            ra.addFlashAttribute("success", "Đã hủy yêu cầu");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchase-requests";
    }
}