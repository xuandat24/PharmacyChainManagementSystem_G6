package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.Supplier;
import fu.se.pharmacy.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public String listSuppliers(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("suppliers", supplierService.searchSuppliers(search));
        model.addAttribute("search", search);
        return "suppliers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "suppliers/create";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("supplier") Supplier supplier, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "suppliers/create";
        }
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        return "suppliers/edit";
    }

    @PostMapping("/update/{id}")
    public String updateSupplier(@PathVariable("id") Integer id, @Valid @ModelAttribute("supplier") Supplier supplier, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "suppliers/edit";
        }
        supplier.setSupplierId(id);
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable("id") Integer id) {
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }
}
