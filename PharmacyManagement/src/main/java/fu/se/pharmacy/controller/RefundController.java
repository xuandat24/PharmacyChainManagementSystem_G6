package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.RefundRequestDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.service.RefundService;
import fu.se.pharmacy.service.SaleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Hoàn tiền):
 * "Pharmacist hoặc Manager tạo yêu cầu hoàn tiền."
 * "Nếu trong hạn mức, Branch Manager có thể duyệt."
 * "Nếu vượt hạn mức hoặc thanh toán online: Admin phải duyệt."
 * → Tạo yêu cầu: Pharmacist + BranchManager.
 * → Duyệt trong hạn mức (approve/{id}): chỉ BranchManager (service tự chặn nếu vượt hạn mức/online).
 * → Duyệt ngoại lệ (approve-admin/{id}): chỉ Admin.
 * → Từ chối: Admin hoặc BranchManager (cả hai đều có quyền giám sát quy trình).
 */
@Controller
@RequestMapping("/refunds")
public class RefundController {

    @Autowired private RefundService refundService;
    @Autowired private SaleService saleService;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        // FIX: thêm requireLogin — trước đây không check gì
        AuthInterceptor.requireLogin(session);
        model.addAttribute("refunds", refundService.findAll());
        return "refunds/history";
    }

    @GetMapping("/pending")
    public String pending(HttpSession session, Model model) {
        // FIX: chỉ Admin/BranchManager xem danh sách chờ duyệt (Pharmacist không duyệt)
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        model.addAttribute("refunds", refundService.findPending());
        model.addAttribute("pendingOnly", true);
        return "refunds/history";
    }

    @GetMapping("/create/{saleId}")
    public String createForm(@PathVariable Integer saleId, HttpSession session, Model model) {
        // FIX: chỉ Pharmacist hoặc BranchManager được tạo yêu cầu hoàn tiền
        AuthInterceptor.requireRole(session, "Pharmacist", "BranchManager");
        saleService.findById(saleId).ifPresent(s -> model.addAttribute("sale", s));
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setSaleId(saleId);
        model.addAttribute("refundDTO", dto);
        return "refunds/request";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("refundDTO") RefundRequestDTO dto,
                         BindingResult result,
                         HttpSession session,
                         Model model,
                         RedirectAttributes ra) {
        AppUser user = AuthInterceptor.requireRole(session, "Pharmacist", "BranchManager");
        if (result.hasErrors()) {
            saleService.findById(dto.getSaleId()).ifPresent(s -> model.addAttribute("sale", s));
            return "refunds/request";
        }
        try {
            refundService.createRequest(dto, user.getUserId());
            ra.addFlashAttribute("success", "Đã gửi yêu cầu hoàn tiền. Chờ duyệt.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/approve/{id}")
    public String approveManager(@PathVariable Integer id,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        // FIX: "Nếu trong hạn mức, Branch Manager có thể duyệt" → chỉ BranchManager
        // (RefundServiceImpl.approveByManager đã tự chặn nếu vượt hạn mức/online)
        AppUser user = AuthInterceptor.requireRole(session, "BranchManager");
        try {
            refundService.approveByManager(id, user.getUserId());
            ra.addFlashAttribute("success", "Đã duyệt hoàn tiền");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/approve-admin/{id}")
    public String approveAdmin(@PathVariable Integer id,
                               HttpSession session,
                               RedirectAttributes ra) {
        // FIX: "Nếu vượt hạn mức hoặc thanh toán online: Admin phải duyệt" → chỉ Admin
        AppUser user = AuthInterceptor.requireRole(session, "Admin");
        try {
            refundService.approveByAdmin(id, user.getUserId());
            ra.addFlashAttribute("success", "Admin đã duyệt hoàn tiền. Tồn kho đã được khôi phục.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Integer id,
                         HttpSession session,
                         RedirectAttributes ra) {
        // FIX: từ chối thuộc quyền giám sát của Admin/BranchManager, không phải Pharmacist
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        try {
            refundService.reject(id, user.getUserId());
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu hoàn tiền");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }
}
