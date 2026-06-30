package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.Employee;
import fu.se.pharmacy.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {

        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);

        // Kiểm tra xem user có tồn tại và đúng pass không
        if (employeeOpt.isPresent() && employeeOpt.get().getPasswordHash().equals(password)) {

            Employee employee = employeeOpt.get();

            // BƯỚC BẢO MẬT THÊM VÀO: Kiểm tra trạng thái của nhân viên
            if (!"ACTIVE".equals(employee.getStatus())) {
                model.addAttribute("error", "Tài khoản đã bị khóa hoặc ngừng hoạt động!");
                return "auth/login"; // Đá văng về trang đăng nhập
            }

            // Nếu qua được ải trên (tức là ACTIVE) thì mới cho đăng nhập
            session.setAttribute("loggedInUser", employee);
            return "redirect:/";

        } else {
            model.addAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("loggedInUser");
        return "auth/login";
    }
}