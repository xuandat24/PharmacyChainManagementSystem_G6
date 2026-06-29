package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.StockCountForm;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.StockCount;
import fu.se.pharmacy.entity.StockCountDetail;
import fu.se.pharmacy.service.StockCountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/stock-counts")
public class StockCountController {

    @Autowired private StockCountService stockCountService;

    @GetMapping
    public String listStockCounts(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        List<StockCount> counts;
        if (user.isAdmin()) {
            // Admin xem TẤT CẢ, không chỉ pending
            counts = stockCountService.getAllStockCounts();
        } else {
            counts = stockCountService.getStockCountsByBranch(user.getBranchId());
        }

        model.addAttribute("counts", counts);
        model.addAttribute("user", user);
        return "stock-counts/list";
    }

    @GetMapping("/create")
    public String createStockCount(HttpSession session, RedirectAttributes ra) {
        // Chỉ BranchManager tạo được — nếu role khác → trang 403
        AuthInterceptor.requireRole(session, "BranchManager");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user.getBranchId() == null) {
            ra.addFlashAttribute("error", "Tài khoản không gắn chi nhánh, không thể tạo kiểm kê.");
            return "redirect:/stock-counts";
        }
        StockCount created = stockCountService.createStockCount(user.getBranchId(), user.getEmployeeId());
        return "redirect:/stock-counts/edit/" + created.getStockCountId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "BranchManager");
        StockCount count = stockCountService.getStockCountById(id);
        if (!"DRAFT".equals(count.getStatus())) return "redirect:/stock-counts";

        StockCountForm form = new StockCountForm();
        form.setStockCountId(id);
        for (StockCountDetail d : count.getDetails()) {
            StockCountForm.Item item = new StockCountForm.Item();
            item.setDetailId(d.getStockCountDetailId());
            item.setActualQuantity(d.getActualQuantity());
            item.setReason(d.getReason());
            form.getItems().add(item);
        }

        model.addAttribute("form", form);
        model.addAttribute("count", count);
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        return "stock-counts/edit";
    }

    @PostMapping("/save")
    public String saveStockCount(@Valid @ModelAttribute("form") StockCountForm form,
                                 BindingResult result, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "BranchManager");
        StockCount count = stockCountService.getStockCountById(form.getStockCountId());
        if (result.hasErrors()) {
            model.addAttribute("count", count);
            return "stock-counts/edit";
        }

        for (StockCountDetail dbDetail : count.getDetails()) {
            for (StockCountForm.Item item : form.getItems()) {
                if (dbDetail.getStockCountDetailId().equals(item.getDetailId())) {
                    dbDetail.setActualQuantity(item.getActualQuantity());
                    dbDetail.setReason(item.getReason());
                }
            }
        }
        stockCountService.saveStockCount(count);
        stockCountService.submitStockCount(count.getStockCountId());
        return "redirect:/stock-counts";
    }

    @GetMapping("/admin/pending")
    public String showPendingCounts(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("counts", stockCountService.getPendingApprovalStockCounts());
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        return "stock-counts/pending";
    }

    @GetMapping("/admin/approve/{id}")
    public String showApproveCountForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        StockCount count = stockCountService.getStockCountById(id);
        model.addAttribute("count", count);
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        return "stock-counts/approve";
    }

    @PostMapping("/admin/approve/{id}")
    public String approveCount(@PathVariable Integer id,
                               @RequestParam(required = false) String adminNotes,
                               HttpSession session,
                               RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        stockCountService.approveStockCount(id, user.getEmployeeId(), adminNotes);
        ra.addFlashAttribute("success", "Đã phê duyệt đợt kiểm kê");
        return "redirect:/stock-counts";
    }

    @PostMapping("/admin/reject/{id}")
    public String rejectCount(@PathVariable Integer id,
                              @RequestParam(required = false) String adminNotes,
                              HttpSession session,
                              RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        stockCountService.rejectStockCount(id, user.getEmployeeId(), adminNotes);
        ra.addFlashAttribute("success", "Đã bác bỏ đợt kiểm kê");
        return "redirect:/stock-counts";
    }
}