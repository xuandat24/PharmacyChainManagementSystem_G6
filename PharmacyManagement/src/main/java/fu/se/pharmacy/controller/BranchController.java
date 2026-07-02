package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.BranchDTO;
import fu.se.pharmacy.entity.Branch;
import fu.se.pharmacy.service.BranchService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public String listBranches(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("branches", branchService.getAllBranches());
        return "branches/list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("branch", new BranchDTO());
        return "branches/form";
    }

    @PostMapping("/save")
    public String saveBranch(@ModelAttribute("branch") BranchDTO branchDTO,
                             HttpSession session, Model model, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        Branch branch = new Branch();
        branch.setBranchId(branchDTO.getBranchId());
        branch.setBranchCode(branchDTO.getBranchCode());
        branch.setBranchName(branchDTO.getBranchName());
        branch.setAddress(branchDTO.getAddress());
        branch.setPhone(branchDTO.getPhone());
        branch.setStatus(branchDTO.getStatus() != null ? branchDTO.getStatus() : "ACTIVE");
        try {
            branchService.saveBranch(branch);
            ra.addFlashAttribute("success", "Da luu chi nhanh thanh cong");
            return "redirect:/branches";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Da co ma chi nhanh nay roi! Vui long nhap ma khac.");
            model.addAttribute("branch", branchDTO);
            return "branches/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        Branch branch = branchService.getBranchById(id);
        if (branch != null) {
            BranchDTO dto = new BranchDTO();
            dto.setBranchId(branch.getBranchId());
            dto.setBranchCode(branch.getBranchCode());
            dto.setBranchName(branch.getBranchName());
            dto.setAddress(branch.getAddress());
            dto.setPhone(branch.getPhone());
            dto.setStatus(branch.getStatus());
            model.addAttribute("branch", dto);
            return "branches/form";
        }
        return "redirect:/branches";
    }

    @GetMapping("/delete/{id}")
    public String deleteBranch(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        try {
            branchService.deleteBranch(id);
            ra.addFlashAttribute("success", "Da vo hieu hoa chi nhanh");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Khong the xoa chi nhanh dang co nhan vien/giao dich lien ket.");
        }
        return "redirect:/branches";
    }
}
