package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.MedicineDTO;
import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.service.CategoryService;
import fu.se.pharmacy.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final CategoryService categoryService;

    @GetMapping
    public String listMedicines(Model model) {
        model.addAttribute("medicines", medicineService.getAllMedicines());
        return "medicines/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("medicine", new MedicineDTO());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "medicines/form";
    }

    @PostMapping("/save")
    public String saveMedicine(@ModelAttribute("medicine") MedicineDTO dto, Model model) {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(dto.getMedicineId());
        medicine.setMedicineCode(dto.getMedicineCode());
        medicine.setMedicineName(dto.getMedicineName());
        medicine.setCategoryId(dto.getCategoryId());
        medicine.setUnit(dto.getUnit());
        medicine.setPrice(dto.getPrice());
        medicine.setDescription(dto.getDescription());
        medicine.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        try {
            // Cố gắng lưu thuốc vào DB
            medicineService.saveMedicine(medicine);
            return "redirect:/medicines";

        } catch (DataIntegrityViolationException e) {
            // BẮT LỖI TRÙNG MÃ THUỐC
            model.addAttribute("error", "Mã thuốc này đã tồn tại! Vui lòng nhập mã khác.");

            // Giữ lại dữ liệu người dùng vừa nhập
            model.addAttribute("medicine", dto);

            // QUAN TRỌNG: Load lại danh sách Category cho Dropdown để không bị lỗi trắng form
            model.addAttribute("categories", categoryService.getAllCategories());

            return "medicines/form";
        }
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Medicine medicine = medicineService.getMedicineById(id);
        if (medicine != null) {
            MedicineDTO dto = new MedicineDTO();
            dto.setMedicineId(medicine.getMedicineId());
            dto.setMedicineCode(medicine.getMedicineCode());
            dto.setMedicineName(medicine.getMedicineName());
            dto.setCategoryId(medicine.getCategoryId());
            dto.setUnit(medicine.getUnit());
            dto.setPrice(medicine.getPrice());
            dto.setDescription(medicine.getDescription());
            dto.setStatus(medicine.getStatus());

            model.addAttribute("medicine", dto);
            // Phải load lại danh sách Loại Thuốc để Dropdown hiển thị
            model.addAttribute("categories", categoryService.getAllCategories());
            return "medicines/form";
        }
        return "redirect:/medicines";
    }

    // Nút Xóa: Chuyển trạng thái thành INACTIVE
    @GetMapping("/delete/{id}")
    public String deleteMedicine(@PathVariable("id") Integer id) {
        medicineService.deleteMedicine(id);
        return "redirect:/medicines";
    }
}