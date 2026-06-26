package fu.se.pharmacy.controller;

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

@Controller
@RequestMapping("/refunds")
public class RefundController {

    @Autowired private RefundService refundService;
    @Autowired private SaleService saleService;

    private AppUser getUser(HttpSession session) {
        return (AppUser) session.getAttribute("loggedInUser");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("refunds", refundService.findAll());
        return "refunds/list";
    }

    @GetMapping("/create/{saleId}")
    public String createForm(@PathVariable Integer saleId, Model model) {
        saleService.findById(saleId).ifPresent(s -> model.addAttribute("sale", s));
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setSaleId(saleId);
        model.addAttribute("refundDTO", dto);
        return "refunds/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("refundDTO") RefundRequestDTO dto,
                         BindingResult result,
                         HttpSession session,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            saleService.findById(dto.getSaleId()).ifPresent(s -> model.addAttribute("sale", s));
            return "refund/create";
        }
        try {
            refundService.createRequest(dto, getUser(session).getUserId());
            ra.addFlashAttribute("success", "Đã gửi yêu cầu hoàn tiền");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/approve/{id}")
    public String approveManager(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            refundService.approveByManager(id, getUser(session).getUserId());
            ra.addFlashAttribute("success", "Đã duyệt hoàn tiền");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refunds";
    }

    @PostMapping("/approve-admin/{id}")
    public String approveAdmin(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        refundService.approveByAdmin(id, getUser(session).getUserId());
        ra.addFlashAttribute("success", "Admin đã duyệt hoàn tiền");
        return "redirect:/refunds";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        refundService.reject(id, getUser(session).getUserId());
        ra.addFlashAttribute("success", "Đã từ chối yêu cầu");
        return "redirect:/refunds";
    }
}
