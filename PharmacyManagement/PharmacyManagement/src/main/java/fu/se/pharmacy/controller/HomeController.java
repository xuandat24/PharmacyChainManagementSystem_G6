package fu.se.pharmacy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index(Model model) {
        // 1. Tạo dữ liệu thuốc giả lập (Không cần gọi Database)
        List<Map<String, Object>> dummyMedicines = new ArrayList<>();

        dummyMedicines.add(createMedicine("MED-2026-01", "Paracetamol 500mg", "Giảm đau, hạ sốt", "Hộp", 45000));
        dummyMedicines.add(createMedicine("MED-2026-02", "Amoxicillin 500mg", "Kháng sinh", "Vỉ", 85000));
        dummyMedicines.add(createMedicine("MED-2026-03", "Panadol Extra", "Giảm đau đầu", "Hộp", 62000));
        dummyMedicines.add(createMedicine("MED-2026-04", "Augmentin 1g", "Kháng sinh liều cao", "Hộp", 280000));
        dummyMedicines.add(createMedicine("MED-2026-05", "Vitamin C 1000mg", "Thực phẩm chức năng", "Tuýp viên sủi", 35000));

        // 2. Đẩy dữ liệu ra giao diện Thymeleaf
        model.addAttribute("medicines", dummyMedicines);
        model.addAttribute("totalMedicines", dummyMedicines.size());

        return "index"; // Mở file templates/index.html
    }

    // Hàm bổ trợ tạo nhanh Map dữ liệu
    private Map<String, Object> createMedicine(String id, String name, String category, String unit, double price) {
        Map<String, Object> med = new HashMap<>();
        med.put("id", id);
        med.put("name", name);
        med.put("category", category);
        med.put("unit", unit);
        med.put("price", price);
        return med;
    }
}
