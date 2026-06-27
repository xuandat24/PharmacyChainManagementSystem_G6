package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.repository.MedicineRepository;
import fu.se.pharmacy.service.CustomerService;
import fu.se.pharmacy.service.PrescriptionService;
import fu.se.pharmacy.service.SaleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sales")
public class SaleController {

    @Autowired private SaleService saleService;
    @Autowired private CustomerService customerService;
    @Autowired private PrescriptionService prescriptionService;
    @Autowired private MedicineRepository medicineRepository;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    /** Màn hình giỏ hàng / POS chính */
    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        AppUser user = getUser(session);
        if (user == null) return "redirect:/login";

        SaleDTO draft = saleService.getOrCreateDraft(user.getUserId(), user.getBranchId());
        List<Medicine> medicines = medicineRepository.findByStatus("ACTIVE");

        model.addAttribute("sale", draft);
        model.addAttribute("medicines", medicines);

        if (draft.getCustomerId() != null)
            customerService.findById(draft.getCustomerId())
                    .ifPresent(c -> model.addAttribute("selectedCustomer", c));

        if (draft.getPrescriptionId() != null)
            prescriptionService.findById(draft.getPrescriptionId())
                    .ifPresent(p -> model.addAttribute("selectedPrescription", p));

        return "sales/cart";
    }

    /** Thêm thuốc vào đơn */
    @PostMapping("/cart/add")
    public String addItem(@RequestParam Integer saleId,
                          @RequestParam Integer medicineId,
                          @RequestParam(defaultValue = "1") Integer quantity,
                          RedirectAttributes ra) {
        String error = saleService.addItem(saleId, medicineId, quantity);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("success", "Đã thêm thuốc vào đơn");
        return "redirect:/sales/cart";
    }

    /** Cập nhật số lượng */
    @PostMapping("/cart/update")
    public String updateItem(@RequestParam Integer saleDetailId,
                             @RequestParam Integer quantity,
                             RedirectAttributes ra) {
        try {
            saleService.updateItemQuantity(saleDetailId, quantity);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/cart";
    }

    /** Xóa dòng thuốc */
    @PostMapping("/cart/remove")
    public String removeItem(@RequestParam Integer saleDetailId) {
        saleService.removeItem(saleDetailId);
        return "redirect:/sales/cart";
    }

    /** Chọn khách hàng */
    @PostMapping("/cart/set-customer")
    public String setCustomer(@RequestParam Integer saleId,
                              @RequestParam Integer customerId,
                              RedirectAttributes ra) {
        saleService.setCustomer(saleId, customerId);
        ra.addFlashAttribute("success", "Đã chọn khách hàng");
        return "redirect:/sales/cart";
    }

    /** Chọn đơn thuốc */
    @PostMapping("/cart/set-prescription")
    public String setPrescription(@RequestParam Integer saleId,
                                  @RequestParam Integer prescriptionId,
                                  RedirectAttributes ra) {
        saleService.setPrescription(saleId, prescriptionId);
        ra.addFlashAttribute("success", "Đã chọn đơn thuốc");
        return "redirect:/sales/cart";
    }

    /** Hủy đơn DRAFT */
    @PostMapping("/cart/cancel")
    public String cancelDraft(@RequestParam Integer saleId, RedirectAttributes ra) {
        try {
            saleService.cancelDraft(saleId);
            ra.addFlashAttribute("success", "Đã hủy hóa đơn");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/cart";
    }

    /** Danh sách hóa đơn */
    @GetMapping
    public String list(HttpSession session, Model model) {
        AppUser user = getUser(session);
        if (user == null) return "redirect:/login";
        // Admin và Manager xem theo chi nhánh; Pharmacist xem theo chi nhánh mình
        model.addAttribute("sales", saleService.findByBranchId(user.getBranchId()));
        return "sales/history";
    }

    /** Chi tiết / in hóa đơn */
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        saleService.findById(id).ifPresent(s -> model.addAttribute("sale", s));
        return "sales/invoice";
    }
}