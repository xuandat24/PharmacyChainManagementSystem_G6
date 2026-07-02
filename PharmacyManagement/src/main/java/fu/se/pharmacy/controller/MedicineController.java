package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.MedicineDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.service.CategoryService;
import fu.se.pharmacy.service.MedicineService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final CategoryService categoryService;

    @GetMapping
    public String listMedicines(HttpSession session, Model model) {
        AuthInterceptor.requireLogin(session);
        model.addAttribute("medicines", medicineService.getAllMedicines());
        return "medicines/list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("medicine", new MedicineDTO());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "medicines/form";
    }

    @PostMapping("/save")
    public String saveMedicine(@ModelAttribute("medicine") MedicineDTO dto,
                               HttpSession session, Model model, RedirectAttributes ra) {
        AppUser admin = AuthInterceptor.requireRole(session, "Admin");
        Medicine medicine = new Medicine();
        medicine.setMedicineId(dto.getMedicineId());
        medicine.setMedicineCode(dto.getMedicineCode());
        medicine.setBarcode(dto.getBarcode());
        medicine.setMedicineName(dto.getMedicineName());
        medicine.setCategoryId(dto.getCategoryId());
        medicine.setActiveIngredient(dto.getActiveIngredient());
        medicine.setStrength(dto.getStrength());
        medicine.setDosageForm(dto.getDosageForm());
        medicine.setUnit(dto.getUnit());
        medicine.setManufacturer(dto.getManufacturer());
        medicine.setCountryOfOrigin(dto.getCountryOfOrigin());
        medicine.setSalePrice(dto.getSalePrice() != null ? dto.getSalePrice() : 0);
        medicine.setMinStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : 0);
        medicine.setRequiresPrescription(dto.getRequiresPrescription() != null ? dto.getRequiresPrescription() : false);
        medicine.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        try {
            medicineService.saveMedicine(medicine, admin.getUserId());
            ra.addFlashAttribute("success", "Da luu thong tin thuoc");
            return "redirect:/medicines";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Ma thuoc nay da ton tai! Vui long nhap ma khac.");
            model.addAttribute("medicine", dto);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "medicines/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        Medicine medicine = medicineService.getMedicineById(id);
        if (medicine != null) {
            MedicineDTO dto = new MedicineDTO();
            dto.setMedicineId(medicine.getMedicineId());
            dto.setMedicineCode(medicine.getMedicineCode());
            dto.setBarcode(medicine.getBarcode());
            dto.setMedicineName(medicine.getMedicineName());
            dto.setCategoryId(medicine.getCategoryId());
            dto.setActiveIngredient(medicine.getActiveIngredient());
            dto.setStrength(medicine.getStrength());
            dto.setDosageForm(medicine.getDosageForm());
            dto.setUnit(medicine.getUnit());
            dto.setManufacturer(medicine.getManufacturer());
            dto.setCountryOfOrigin(medicine.getCountryOfOrigin());
            dto.setSalePrice(medicine.getSalePrice());
            dto.setMinStockLevel(medicine.getMinStockLevel());
            dto.setRequiresPrescription(medicine.getRequiresPrescription());
            dto.setStatus(medicine.getStatus());
            model.addAttribute("medicine", dto);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "medicines/form";
        }
        return "redirect:/medicines";
    }

    @GetMapping("/delete/{id}")
    public String deleteMedicine(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        medicineService.deleteMedicine(id);
        ra.addFlashAttribute("success", "Da vo hieu hoa thuoc");
        return "redirect:/medicines";
    }
}
