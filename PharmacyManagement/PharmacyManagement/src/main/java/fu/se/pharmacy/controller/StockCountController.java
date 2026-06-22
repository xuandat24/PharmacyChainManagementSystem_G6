package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.StockCountForm;
import fu.se.pharmacy.entity.Employee;
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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/stock-counts")
public class StockCountController {

    @Autowired
    private StockCountService stockCountService;

    @GetMapping
    public String listStockCounts(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<StockCount> counts;
        if ("Admin".equals(user.getRole())) {
            // Tạm thời lấy hết hoặc lấy theo chi nhánh của Admin
            counts = stockCountService.getStockCountsByBranch(user.getBranch().getBranchId());
        } else {
            counts = stockCountService.getStockCountsByBranch(user.getBranch().getBranchId());
        }

        model.addAttribute("counts", counts);
        model.addAttribute("user", user);
        return "stock-counts/list";
    }

    @GetMapping("/create")
    public String createStockCount(HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        StockCount created = stockCountService.createStockCount(user.getBranch().getBranchId(), user.getEmployeeId());
        return "redirect:/stock-counts/edit/" + created.getStockCountId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        StockCount count = stockCountService.getStockCountById(id);
        if (!"DRAFT".equals(count.getStatus())) {
            return "redirect:/stock-counts";
        }

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
        return "stock-counts/edit";
    }

    @PostMapping("/save")
    public String saveStockCount(@Valid @ModelAttribute("form") StockCountForm form, BindingResult result, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"BranchManager".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        StockCount count = stockCountService.getStockCountById(form.getStockCountId());

        if (result.hasErrors()) {
            model.addAttribute("count", count);
            return "stock-counts/edit";
        }

        List<StockCountDetail> details = new ArrayList<>();
        for (StockCountForm.Item item : form.getItems()) {
            StockCountDetail d = new StockCountDetail();
            d.setStockCountDetailId(item.getDetailId());
            d.setActualQuantity(item.getActualQuantity());
            d.setReason(item.getReason());
            details.add(d);
        }

        // Cập nhật chi tiết kiểm kê
        for (StockCountDetail dbDetail : count.getDetails()) {
            for (StockCountDetail formDetail : details) {
                if (dbDetail.getStockCountDetailId().equals(formDetail.getStockCountDetailId())) {
                    dbDetail.setActualQuantity(formDetail.getActualQuantity());
                    dbDetail.setDifference(formDetail.getActualQuantity() - dbDetail.getSystemQuantity());
                    dbDetail.setReason(formDetail.getReason());
                }
            }
        }

        stockCountService.saveStockCount(count);
        stockCountService.submitStockCount(count.getStockCountId());

        return "redirect:/stock-counts";
    }

    @GetMapping("/admin/pending")
    public String showPendingCounts(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        model.addAttribute("counts", stockCountService.getPendingApprovalStockCounts());
        return "stock-counts/pending";
    }

    @GetMapping("/admin/approve/{id}")
    public String showApproveCountForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        StockCount count = stockCountService.getStockCountById(id);
        if (!"PENDING_ADMIN_APPROVAL".equals(count.getStatus())) {
            return "redirect:/stock-counts";
        }

        model.addAttribute("count", count);
        return "stock-counts/approve";
    }

    @PostMapping("/admin/approve/{id}")
    public String approveCount(@PathVariable("id") Integer id,
                               @RequestParam(value = "adminNotes", required = false) String adminNotes,
                               HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        stockCountService.approveStockCount(id, user.getEmployeeId(), adminNotes);
        return "redirect:/stock-counts";
    }

    @PostMapping("/admin/reject/{id}")
    public String rejectCount(@PathVariable("id") Integer id,
                              @RequestParam(value = "adminNotes", required = false) String adminNotes,
                              HttpSession session) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/stock-counts";
        }

        stockCountService.rejectStockCount(id, user.getEmployeeId(), adminNotes);
        return "redirect:/stock-counts";
    }
}
