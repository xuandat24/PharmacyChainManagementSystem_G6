package fu.se.pharmacy.controller;

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

@Controller
@RequestMapping("/prescriptions")
public class PrescriptionController {

    @Autowired private PrescriptionService prescriptionService;
    @Autowired private CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer customerId, Model model) {
        if (customerId != null) {
            model.addAttribute("prescriptions", prescriptionService.findByCustomerId(customerId));
            customerService.findById(customerId).ifPresent(c -> model.addAttribute("customer", c));
            model.addAttribute("customerId", customerId);
        } else {
            // BUG FIX 1: Khi không có customerId, prescriptions=null → th:each NPE
            // Truyền danh sách rỗng để template không bị lỗi
            model.addAttribute("prescriptions", List.of());
        }
        return "prescriptions/list";
    }

    @GetMapping("/create")
    public String showCreate(@RequestParam Integer customerId, Model model) {
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
                             Model model) {  // BUG FIX 2: thêm Model parameter
        if (result.hasErrors()) {
            // BUG FIX 2: khi validation fail, template cần ${customer} để hiển thị alert
            // Nếu thiếu → header alert bị trắng, th:text="${customer.fullName}" NPE
            if (dto.getCustomerId() != null) {
                customerService.findById(dto.getCustomerId())
                        .ifPresent(c -> model.addAttribute("customer", c));
            }
            return "prescriptions/create";
        }
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        PrescriptionDTO saved = prescriptionService.save(dto, user.getUserId());
        return "redirect:/prescriptions/" + saved.getPrescriptionId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        PrescriptionDTO p = prescriptionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuốc"));
        model.addAttribute("prescription", p);
        model.addAttribute("details", prescriptionService.findDetails(id));
        customerService.findById(p.getCustomerId()).ifPresent(c -> model.addAttribute("customer", c));
        return "prescriptions/detail";
    }

    @PostMapping("/{id}/add-medicine")
    public String addMedicine(@PathVariable Integer id,
                              @ModelAttribute PrescriptionDetailDTO dto) {
        dto.setPrescriptionId(id);
        prescriptionService.addDetail(dto);
        return "redirect:/prescriptions/" + id;
    }
}