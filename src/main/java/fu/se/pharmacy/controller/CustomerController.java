package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping
    public String listCustomers(Model model) {

        List<Customer> customerList = customerRepository.findAll();

        model.addAttribute("customers", customerList);

        return "customer/list";
    }
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // Tạo một vỏ bọc Khách hàng rỗng để hứng dữ liệu từ màn hình truyền về
        model.addAttribute("customer", new Customer());
        return "customer/create";
    }

    @PostMapping("/create")
    public String saveCustomer(@ModelAttribute("customer") Customer customer) {
        customerRepository.save(customer);
        return "redirect:/customers";
    }
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID khách hàng không hợp lệ: " + id));

        model.addAttribute("customer", customer);
        return "customer/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateCustomer(@PathVariable("id") Integer id, @ModelAttribute("customer") Customer customer) {
        customer.setCustomerId(id);

        customerRepository.save(customer);
        return "redirect:/customers";
    }
}