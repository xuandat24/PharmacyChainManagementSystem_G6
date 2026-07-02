package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.CategoryDTO;
import fu.se.pharmacy.entity.Category;
import fu.se.pharmacy.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("categories", categoryService.getAllCategories());
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("category", new CategoryDTO());
        return "categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") CategoryDTO dto,
                               HttpSession session, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        Category category = new Category();
        category.setCategoryId(dto.getCategoryId());
        category.setCategoryName(dto.getCategoryName());
        category.setDescription(dto.getDescription());
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        categoryService.saveCategory(category);
        ra.addFlashAttribute("success", "Da luu danh muc thanh cong");
        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            CategoryDTO dto = new CategoryDTO();
            dto.setCategoryId(category.getCategoryId());
            dto.setCategoryName(category.getCategoryName());
            dto.setDescription(category.getDescription());
            dto.setStatus(category.getStatus());
            model.addAttribute("category", dto);
            return "categories/form";
        }
        return "redirect:/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        try {
            categoryService.deleteCategory(id);
            ra.addFlashAttribute("success", "Da vo hieu hoa danh muc");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Khong the xoa danh muc dang co thuoc lien ket.");
        }
        return "redirect:/categories";
    }
}
