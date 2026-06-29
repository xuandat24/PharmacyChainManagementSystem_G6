package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.entity.Supplier;
import fu.se.pharmacy.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired private SupplierService supplierService;

    @GetMapping
    public String listSuppliers(@RequestParam(value = "search", required = false) String search,
                                HttpSession session,
                                Model model) {
        // Tất cả role đều xem được danh sách NCC
        AuthInterceptor.requireLogin(session);
        model.addAttribute("suppliers", supplierService.searchSuppliers(search));
        model.addAttribute("search", search);
        return "suppliers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        // Chỉ Admin và BranchManager tạo được
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        model.addAttribute("supplier", new Supplier());
        return "suppliers/create";
    }

    @PostMapping("/save")
    public String saveSupplier(HttpSession session,
                               @Valid @ModelAttribute("supplier") Supplier supplier,
                               BindingResult result,
                               Model model) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        if (result.hasErrors()) return "suppliers/create";
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               HttpSession session,
                               Model model) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        return "suppliers/edit";
    }

    @PostMapping("/update/{id}")
    public String updateSupplier(@PathVariable Integer id,
                                 HttpSession session,
                                 @Valid @ModelAttribute("supplier") Supplier supplier,
                                 BindingResult result,
                                 Model model) {
        AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        if (result.hasErrors()) return "suppliers/edit";
        supplier.setSupplierId(id);
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Integer id, HttpSession session) {
        // Chỉ Admin xóa được
        AuthInterceptor.requireRole(session, "Admin");
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }
}