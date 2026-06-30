package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.CustomerDTO;
import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Khách hàng):
 * "Pharmacist tìm khách theo số điện thoại → tạo khách hàng mới nếu chưa có."
 * → CHỈ Pharmacist được tạo/sửa khách hàng.
 * Admin và BranchManager không trực tiếp vận hành bán hàng nên chỉ xem (read-only).
 */
@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired private CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) String search, HttpSession session, Model model) {
        // FIX: trước đây không check login — bất kỳ ai (kể cả chưa đăng nhập) cũng xem được
        AuthInterceptor.requireLogin(session);

        List<CustomerDTO> customers;
        if (search != null && !search.isBlank()) {
            Optional<Customer> byPhone = customerService.findByPhone(search.trim());
            customers = byPhone.map(c -> {
                CustomerDTO r = new CustomerDTO();
                r.setCustomerId(c.getCustomerId()); r.setFullName(c.getFullName());
                r.setPhone(c.getPhone()); r.setDateOfBirth(c.getDateOfBirth());
                r.setGender(c.getGender()); r.setAddress(c.getAddress());
                r.setAllergyNote(c.getAllergyNote()); r.setCreatedAt(c.getCreatedAt());
                return List.of(r);
            }).orElseGet(() -> customerService.searchByName(search.trim()));
            model.addAttribute("search", search);
        } else {
            customers = customerService.findAll();
        }
        model.addAttribute("customers", customers);
        return "customers/list";
    }

    @GetMapping("/create")
    public String showCreate(HttpSession session, Model model) {
        // FIX: chỉ Pharmacist được tạo khách hàng (đúng nghiệp vụ "Pharmacist tìm/tạo khách")
        AuthInterceptor.requireRole(session, "Pharmacist");
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customers/create";
    }

    @PostMapping("/create")
    public String saveCreate(HttpSession session,
                             @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                             BindingResult result) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        if (result.hasErrors()) return "customers/create";
        customerService.save(dto);
        return "redirect:/customers";
    }

    @GetMapping("/edit/{id}")
    public String showEdit(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        CustomerDTO resp = customerService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(resp.getCustomerId()); dto.setFullName(resp.getFullName());
        dto.setPhone(resp.getPhone()); dto.setDateOfBirth(resp.getDateOfBirth());
        dto.setGender(resp.getGender()); dto.setAddress(resp.getAddress());
        dto.setAllergyNote(resp.getAllergyNote());
        model.addAttribute("customerDTO", dto);
        return "customers/edit";
    }

    @PostMapping("/edit/{id}")
    public String saveEdit(@PathVariable Integer id,
                           HttpSession session,
                           @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                           BindingResult result) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        if (result.hasErrors()) return "customers/edit";
        dto.setCustomerId(id);
        customerService.save(dto);
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireLogin(session);
        model.addAttribute("customer", customerService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng")));
        return "customers/detail";
    }

    @GetMapping("/search")
    @ResponseBody
    public Customer searchByPhone(@RequestParam String phone, HttpSession session) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        return customerService.findByPhone(phone).orElse(null);
    }
}
