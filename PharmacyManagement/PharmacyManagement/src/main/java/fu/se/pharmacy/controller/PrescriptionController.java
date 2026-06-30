package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.PrescriptionDetailDTO;
import fu.se.pharmacy.dto.PrescriptionDTO;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.service.CustomerService;
import fu.se.pharmacy.service.PrescriptionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Đơn thuốc):
 * "Dược sĩ chỉ ghi nhận đơn do khách cung cấp, không tự kê đơn thay bác sĩ."
 * → CHỈ Pharmacist được tạo/thêm thuốc vào đơn thuốc.
 * Admin/BranchManager chỉ xem để phục vụ tra cứu khi cần.
 */
@Controller
@RequestMapping("/prescriptions")
public class PrescriptionController {

    @Autowired private PrescriptionService prescriptionService;
    @Autowired private CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer customerId, HttpSession session, Model model) {
        // FIX: thêm requireLogin — trước đây không check gì cả
        AuthInterceptor.requireLogin(session);

        if (customerId != null) {
            model.addAttribute("prescriptions", prescriptionService.findByCustomerId(customerId));
            customerService.findById(customerId).ifPresent(c -> model.addAttribute("customer", c));
            model.addAttribute("customerId", customerId);
        } else {
            model.addAttribute("prescriptions", List.of());
        }
        return "prescriptions/list";
    }

    @GetMapping("/create")
    public String showCreate(@RequestParam Integer customerId, HttpSession session, Model model) {
        // FIX: chỉ Pharmacist được ghi nhận đơn thuốc
        AuthInterceptor.requireRole(session, "Pharmacist");
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setCustomerId(customerId);
        model.addAttribute("prescriptionDTO", dto);
        customerService.findById(customerId).ifPresent(c -> model.addAttribute("customer", c));
        return "prescriptions/create";
    }

    @PostMapping("/create")
    public String saveCreate(@Valid @ModelAttribute("prescriptionDTO") PrescriptionDTO dto,
                             BindingResult result,
                             HttpSession session,
                             Model model) {
        AppUser user = AuthInterceptor.requireRole(session, "Pharmacist");
        if (result.hasErrors()) {
            if (dto.getCustomerId() != null) {
                customerService.findById(dto.getCustomerId())
                        .ifPresent(c -> model.addAttribute("customer", c));
            }
            return "prescriptions/create";
        }
        PrescriptionDTO saved = prescriptionService.save(dto, user.getUserId());
        return "redirect:/prescriptions/" + saved.getPrescriptionId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireLogin(session);
        PrescriptionDTO p = prescriptionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc"));
        model.addAttribute("prescription", p);
        model.addAttribute("details", prescriptionService.findDetails(id));
        customerService.findById(p.getCustomerId()).ifPresent(c -> model.addAttribute("customer", c));
        return "prescriptions/detail";
    }

    @PostMapping("/{id}/add-medicine")
    public String addMedicine(@PathVariable Integer id,
                              HttpSession session,
                              @ModelAttribute PrescriptionDetailDTO dto) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        dto.setPrescriptionId(id);
        prescriptionService.addDetail(dto);
        return "redirect:/prescriptions/" + id;
    }
}
