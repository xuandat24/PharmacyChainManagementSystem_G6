package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.CategoryDTO;
import fu.se.pharmacy.entity.Category;
import fu.se.pharmacy.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor // Fix lỗi java:S6813 (Constructor Injection)
public class CategoryController {

    private final CategoryService categoryService; // Thêm 'final', bỏ @Autowired

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        // Fix lỗi java:S4684 (Ném DTO ra giao diện thay vì Entity)
        model.addAttribute("category", new CategoryDTO());
        return "categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") CategoryDTO categoryDTO) {

        Category category = new Category();
        category.setCategoryId(categoryDTO.getCategoryId());
        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDescription(categoryDTO.getDescription());


        category.setStatus(categoryDTO.getStatus() != null ? categoryDTO.getStatus() : "ACTIVE");

        categoryService.saveCategory(category);
        return "redirect:/categories";
    }
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            CategoryDTO dto = new CategoryDTO();
            dto.setCategoryId(category.getCategoryId());
            dto.setCategoryName(category.getCategoryName());
            dto.setDescription(category.getDescription());
            dto.setStatus(category.getStatus());

            model.addAttribute("category", dto);
            return "categories/form"; // Tái sử dụng lại form Thêm mới!
        }
        return "redirect:/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }
}