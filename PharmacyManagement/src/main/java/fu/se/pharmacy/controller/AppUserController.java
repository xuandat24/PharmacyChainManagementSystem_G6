package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.AppUserDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.Branch;
import fu.se.pharmacy.entity.Role;
import fu.se.pharmacy.service.AppUserService;
import fu.se.pharmacy.service.BranchService;
import fu.se.pharmacy.service.RoleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/appusers")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;
    private final BranchService branchService;
    private final RoleService roleService;

    @GetMapping
    public String listUsers(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("appusers", appUserService.getAllUsers());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("roles", roleService.getAllRoles());
        return "appusers/list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("appUser", new AppUserDTO());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("roles", roleService.getAllRoles());
        return "appusers/form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("appUser") AppUserDTO dto,
                           HttpSession session, Model model, RedirectAttributes ra) {
        AppUser admin = AuthInterceptor.requireRole(session, "Admin");

        AppUser user = new AppUser();
        user.setUserId(dto.getUserId());
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());

        if (dto.getPasswordHash() != null && !dto.getPasswordHash().isEmpty()) {
            user.setPasswordHash(dto.getPasswordHash());
        } else if (dto.getUserId() == null) {
            model.addAttribute("error", "Mật khẩu không được để trống khi tạo mới.");
            model.addAttribute("appUser", dto);
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("roles", roleService.getAllRoles());
            return "appusers/form";
        } else {
            AppUser existing = appUserService.getUserById(dto.getUserId());
            if (existing != null) user.setPasswordHash(existing.getPasswordHash());
        }
        user.setRoleId(dto.getRoleId());
        // Admin không gắn chi nhánh
        user.setBranchId(dto.getRoleId() != null && dto.getRoleId() == 1 ? null : dto.getBranchId());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        try {
            appUserService.saveUser(user);
            ra.addFlashAttribute("success", "Đã lưu tài khoản thành công");
            return "redirect:/appusers";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.");
            model.addAttribute("appUser", dto);
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("roles", roleService.getAllRoles());
            return "appusers/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        AppUser user = appUserService.getUserById(id);
        if (user != null) {
            AppUserDTO dto = new AppUserDTO();
            dto.setUserId(user.getUserId());
            dto.setFullName(user.getFullName());
            dto.setUsername(user.getUsername());
            dto.setPasswordHash(""); // Không hiện pass cũ
            dto.setRoleId(user.getRoleId());
            dto.setBranchId(user.getBranchId());
            dto.setStatus(user.getStatus());
            model.addAttribute("appUser", dto);
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("roles", roleService.getAllRoles());
            return "appusers/form";
        }
        return "redirect:/appusers";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        // FIX: performedByUserId lấy từ session (user đang login), không phải @RequestParam
        // Trước đây: deleteUser(id, performedByUserId) với performedByUserId luôn null vì
        // không được truyền vào URL, khiến audit log ghi null cho trường performed_by.
        AppUser admin = AuthInterceptor.requireRole(session, "Admin");
        try {
            appUserService.deleteUser(id, admin.getUserId());
            ra.addFlashAttribute("success", "Đã khóa tài khoản");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/appusers";
    }
}
