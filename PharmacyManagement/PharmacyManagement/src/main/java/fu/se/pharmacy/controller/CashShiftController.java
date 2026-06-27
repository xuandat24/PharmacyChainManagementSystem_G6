package fu.se.pharmacy.controller;

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

@Controller
@RequestMapping("/cash-shifts")
public class CashShiftController {

    @Autowired private CashShiftService cashShiftService;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    /** Trang chính: hiển thị ca đang mở + lịch sử ca */
    @GetMapping
    public String list(HttpSession session, Model model) {
        AppUser user = getUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("openShift",
                cashShiftService.getOpenShift(user.getUserId()).orElse(null));
        model.addAttribute("allShifts",
                cashShiftService.findByBranchId(user.getBranchId()));
        return "cash-shifts/open"; // FIX: was "cash-shifts/detail" (template trống)
    }

    /** Mở ca mới */
    @PostMapping("/open")
    public String open(HttpSession session, RedirectAttributes ra) {
        AppUser user = getUser(session);
        if (user == null) return "redirect:/login";
        cashShiftService.openShift(user.getUserId(), user.getBranchId());
        ra.addFlashAttribute("success", "Đã mở ca làm việc");
        return "redirect:/cash-shifts";
    }

    /** Form chốt ca */
    @GetMapping("/close/{id}")
    public String closeForm(@PathVariable Integer id, Model model) {
        cashShiftService.findById(id).ifPresent(s -> model.addAttribute("shift", s));
        model.addAttribute("cashShiftDTO", new CashShiftDTO());
        return "cash-shifts/close";
    }

    /** Xử lý chốt ca */
    @PostMapping("/close/{id}")
    public String close(@PathVariable Integer id,
                        @Valid @ModelAttribute("cashShiftDTO") CashShiftDTO dto,
                        BindingResult result,
                        Model model,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            cashShiftService.findById(id).ifPresent(s -> model.addAttribute("shift", s));
            return "cash-shifts/close";
        }
        try {
            CashShiftDTO shift = cashShiftService.closeShift(id, dto);
            int diff = shift.getDifferenceAmount() == null ? 0 : shift.getDifferenceAmount();
            if ("PENDING_ADMIN_REVIEW".equals(shift.getStatus())) {
                ra.addFlashAttribute("warning",
                        "Chênh lệch lớn (" + String.format("%,d", Math.abs(diff)) + " đ). Đang chờ Admin xem xét.");
            } else {
                ra.addFlashAttribute("success",
                        "Đã chốt ca. Chênh lệch: " + String.format("%,d", diff) + " đ. Chờ Manager xác nhận.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash-shifts";
    }

    /** Manager xác nhận ca đã chốt */
    @PostMapping("/confirm/{id}")
    public String confirm(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            cashShiftService.confirmShift(id, getUser(session).getUserId());
            ra.addFlashAttribute("success", "Đã xác nhận ca");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cash-shifts";
    }
}