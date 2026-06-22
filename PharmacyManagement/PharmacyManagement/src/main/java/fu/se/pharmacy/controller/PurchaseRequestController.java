package fu.se.pharmacy.controller;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/purchase-requests")
public class PurchaseRequestController {

    @Autowired
    private PurchaseRequestService prService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private MedicineRepository medicineRepository;

    @GetMapping
    public String listRequests(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<PurchaseRequest> requests;
        if ("Admin".equals(user.getRole())) {
            requests = prService.getAllRequests();
        } else {
            requests = prService.getRequestsByBranch(user.getBranch().getBranchId());
        }

        model.addAttribute("requests", requests);
        model.addAttribute("user", user);
        return "purchase-requests/list";
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }

        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("medicines", medicineRepository.findAll());
        model.addAttribute("form", new PurchaseRequestForm());
        return "purchase-requests/create";
    }

    @PostMapping("/save")
    public String saveRequest(@Valid @ModelAttribute("form") PurchaseRequestForm form, BindingResult result, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }

        if (result.hasErrors() || form.getItems().isEmpty()) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("medicines", medicineRepository.findAll());
            if (form.getItems().isEmpty()) {
                model.addAttribute("error", "Vui lòng thêm ít nhất một mặt hàng yêu cầu nhập.");
            }
            return "purchase-requests/create";
        }

        PurchaseRequest pr = new PurchaseRequest();
        pr.setBranch(user.getBranch());
        pr.setCreatedBy(user);
        pr.setSupplier(supplierService.getSupplierById(form.getSupplierId()));
        pr.setStatus("DRAFT");

        List<PurchaseRequestDetail> details = new ArrayList<>();
        for (PurchaseRequestForm.Item item : form.getItems()) {
            PurchaseRequestDetail detail = new PurchaseRequestDetail();
            detail.setPurchaseRequest(pr);
            detail.setMedicine(medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuốc ID: " + item.getMedicineId())));
            detail.setQuantityRequested(item.getQuantityRequested());
            detail.setQuantityApproved(0); // Chờ Admin duyệt
            detail.setEstimatedPrice(item.getEstimatedPrice());
            details.add(detail);
        }
        pr.setDetails(details);

        prService.saveRequest(pr);
        return "redirect:/purchase-requests";
    }

    @GetMapping("/submit/{id}")
    public String submitRequest(@PathVariable("id") Integer id, HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }
        prService.submitRequest(id);
        return "redirect:/purchase-requests";
    }

    @GetMapping("/admin/approve/{id}")
    public String showApproveForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }

        PurchaseRequest pr = prService.getRequestById(id);
        if (!"SUBMITTED".equals(pr.getStatus())) {
            return "redirect:/purchase-requests";
        }

        model.addAttribute("pr", pr);
        return "purchase-requests/approve";
    }

    @PostMapping("/admin/approve/{id}")
    public String approveRequest(@PathVariable("id") Integer id,
                                 @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                 @RequestParam("detailIds") List<Integer> detailIds,
                                 @RequestParam("approvedQuantities") List<Integer> approvedQuantities,
                                 HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }

        List<PurchaseRequestDetail> approvedDetails = new ArrayList<>();
        for (int i = 0; i < detailIds.size(); i++) {
            PurchaseRequestDetail d = new PurchaseRequestDetail();
            d.setRequestDetailId(detailIds.get(i));
            d.setQuantityApproved(approvedQuantities.get(i));
            approvedDetails.add(d);
        }

        prService.approveRequest(id, user.getEmployeeId(), adminNotes, approvedDetails);
        return "redirect:/purchase-requests";
    }

    @PostMapping("/admin/reject/{id}")
    public String rejectRequest(@PathVariable("id") Integer id,
                                @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }
        prService.rejectRequest(id, user.getEmployeeId(), adminNotes);
        return "redirect:/purchase-requests";
    }

    @GetMapping("/cancel/{id}")
    public String cancelRequest(@PathVariable("id") Integer id, HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/purchase-requests";
        }
        prService.cancelRequest(id);
        return "redirect:/purchase-requests";
    }
}
