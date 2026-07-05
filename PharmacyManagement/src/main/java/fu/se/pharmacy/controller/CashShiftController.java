package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.CashShiftDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.service.CashShiftService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Chốt ca):
 * "Pharmacist nhập tiền mặt thực tế... Branch Manager xác nhận."
 * → Mở ca / đóng ca: chỉ Pharmacist (người trực tiếp bán hàng, giữ tiền mặt).
 * → Xác nhận ca (sau khi Pharmacist đóng): chỉ BranchManager.
 * → Admin chỉ xem để theo dõi, không trực tiếp mở/đóng/xác nhận ca.
 */
@Controller
@RequestMapping("/cash-shifts")
public class CashShiftController {

    @Autowired private CashShiftService cashShiftService;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        model.addAttribute("openShift",
                cashShiftService.getOpenShift(user.getUserId()).orElse(null));

        List<CashShiftDTO> allShifts;
        if (user.isAdmin()) {
            allShifts = cashShiftService.findAll();
        } else {
            allShifts = cashShiftService.findByBranchId(user.getBranchId());
        }
        model.addAttribute("allShifts", allShifts);
        model.addAttribute("user", user);
        return "cash-shifts/open";
    }

    @PostMapping("/open")
    public String open(HttpSession session, RedirectAttributes ra) {
        // FIX: chỉ Pharmacist mở ca (người trực tiếp bán hàng giữ tiền mặt)
        AppUser user = AuthInterceptor.requireRole(session, "Pharmacist");
        if (user.getBranchId() == null) {
            ra.addFlashAttribute("error", "Tài khoản không thuộc chi nhánh, không thể mở ca.");
            return "redirect:/cash-shifts";
        }
        cashShiftService.openShift(user.getUserId(), user.getBranchId());
        ra.addFlashAttribute("success", "Đã mở ca làm việc");
        return "redirect:/cash-shifts";
    }

    @GetMapping("/close/{id}")
    public String closeForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        cashShiftService.findById(id).ifPresent(s -> model.addAttribute("shift", s));
        model.addAttribute("cashShiftDTO", new CashShiftDTO());
        return "cash-shifts/close";
    }

    @PostMapping("/close/{id}")
    public String close(@PathVariable Integer id,
                        HttpSession session,
                        @Valid @ModelAttribute("cashShiftDTO") CashShiftDTO dto,
                        BindingResult result,
                        Model model,
                        RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        if (result.hasErrors()) {
            cashShiftService.findById(id).ifPresent(s -> model.addAttribute("shift", s));
            return "cash-shifts/close";
        }
        try {
            CashShiftDTO shift = cashShiftService.closeShift(id, dto);
            int diff = shift.getDifferenceAmount() == null ? 0 : shift.getDifferenceAmount();
            if ("PENDING_ADMIN_REVIEW".equals(shift.getStatus())) {
                ra.addFlashAttribute("warning",
                        "Chenh lech lon (" + String.format("%,d", Math.abs(diff)) + " d). Dang cho Admin xem xet.");
            } else {
                ra.addFlashAttribute("success",
                        "Da chot ca. Chenh lech: " + String.format("%,d", diff) + " d. Cho Manager xac nhan.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash-shifts";
    }

    @PostMapping("/confirm/{id}")
    public String confirm(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        // FIX: "Branch Manager xac nhan" -> chi BranchManager
        AppUser user = AuthInterceptor.requireRole(session, "BranchManager");
        try {
            cashShiftService.confirmShift(id, user.getUserId());
            ra.addFlashAttribute("success", "Da xac nhan ca");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash-shifts";
    }
}
