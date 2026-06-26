package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.CustomerDTO;
import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired

    private CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
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
    public String showCreate(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customers/create";
    }

    @PostMapping("/create")
    public String saveCreate(@Valid @ModelAttribute("customerDTO") CustomerDTO dto, BindingResult result) {
        if (result.hasErrors()) return "customer/create";
        customerService.save(dto);
        return "redirect:/customers";
    }

    @GetMapping("/edit/{id}")
    public String showEdit(@PathVariable Integer id, Model model) {
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
                           @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                           BindingResult result) {
        if (result.hasErrors()) return "customers/edit";
        dto.setCustomerId(id);
        customerService.save(dto);
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("customer", customerService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng")));
        return "customers/detail";
    }

    @GetMapping("/search")
    @ResponseBody
    public Customer searchByPhone(@RequestParam String phone) {
        return customerService.findByPhone(phone).orElse(null);
    }
}