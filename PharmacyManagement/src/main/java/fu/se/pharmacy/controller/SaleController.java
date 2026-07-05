package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
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

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Bán hàng):
 * "Pharmacist tạo hóa đơn DRAFT, thêm thuốc, thanh toán."
 * "Hóa đơn DRAFT — Pharmacist được tự hủy."
 * → CHỈ Pharmacist được thao tác giỏ hàng (cart/add, cart/update, cart/remove,
 *   cart/set-customer, cart/set-prescription, cart/cancel).
 * Admin/BranchManager chỉ xem lịch sử hóa đơn để giám sát, không trực tiếp bán hàng.
 */
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

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        // FIX: chỉ Pharmacist được vào giỏ hàng/bán hàng
        AppUser user = AuthInterceptor.requireRole(session, "Pharmacist");

        if (user.getBranchId() == null) {
            model.addAttribute("error",
                    "Tài khoản không thuộc chi nhánh nào. Vui lòng liên hệ Admin để được gán chi nhánh.");
            model.addAttribute("sale", null);
            model.addAttribute("medicines", List.of());
            return "sales/cart";
        }

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

    @PostMapping("/cart/add")
    public String addItem(HttpSession session,
                          @RequestParam Integer saleId,
                          @RequestParam Integer medicineId,
                          @RequestParam(defaultValue = "1") Integer quantity,
                          RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        String error = saleService.addItem(saleId, medicineId, quantity);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("success", "Đã thêm thuốc vào đơn");
        return "redirect:/sales/cart";
    }

    @PostMapping("/cart/update")
    public String updateItem(HttpSession session,
                             @RequestParam Integer saleDetailId,
                             @RequestParam Integer quantity,
                             RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        try {
            saleService.updateItemQuantity(saleDetailId, quantity);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(HttpSession session, @RequestParam Integer saleDetailId) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        saleService.removeItem(saleDetailId);
        return "redirect:/sales/cart";
    }

    @PostMapping("/cart/set-customer")
    public String setCustomer(HttpSession session,
                              @RequestParam Integer saleId,
                              @RequestParam Integer customerId,
                              RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        saleService.setCustomer(saleId, customerId);
        ra.addFlashAttribute("success", "Đã chọn khách hàng");
        return "redirect:/sales/cart";
    }

    @PostMapping("/cart/set-prescription")
    public String setPrescription(HttpSession session,
                                  @RequestParam Integer saleId,
                                  @RequestParam Integer prescriptionId,
                                  RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        saleService.setPrescription(saleId, prescriptionId);
        ra.addFlashAttribute("success", "Đã chọn đơn thuốc");
        return "redirect:/sales/cart";
    }

    @PostMapping("/cart/cancel")
    public String cancelDraft(HttpSession session, @RequestParam Integer saleId, RedirectAttributes ra) {
        // FIX: "Hóa đơn DRAFT — Pharmacist được tự hủy" → chỉ Pharmacist
        AuthInterceptor.requireRole(session, "Pharmacist");
        try {
            saleService.cancelDraft(saleId);
            ra.addFlashAttribute("success", "Đã hủy hóa đơn");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/cart";
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        // FIX: thêm requireLogin — lịch sử hóa đơn mọi role đã đăng nhập đều xem được
        // để Admin/BranchManager giám sát, nhưng phải qua check login trước
        AppUser user = AuthInterceptor.requireLogin(session);
        if (user.getBranchId() == null) {
            model.addAttribute("sales", saleService.findAll());
        } else {
            model.addAttribute("sales", saleService.findByBranchId(user.getBranchId()));
        }
        return "sales/history";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireLogin(session);
        saleService.findById(id).ifPresent(s -> model.addAttribute("sale", s));
        return "sales/invoice";
    }
}
