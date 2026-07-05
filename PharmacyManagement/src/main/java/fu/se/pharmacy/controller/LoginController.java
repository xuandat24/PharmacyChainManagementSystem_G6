package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.repository.AppUserRepository;
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
    private AppUserRepository appUserRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {

        Optional<AppUser> employeeOpt = appUserRepository.findByUsername(username);

        // Kiểm tra xem user có tồn tại và đúng pass không
        if (employeeOpt.isEmpty() || !employeeOpt.get().getPasswordHash().equals(password)) {
            model.addAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            return "auth/login";
        }

        AppUser user = employeeOpt.get();

        // FIX: trước đây không kiểm tra trạng thái tài khoản — tài khoản bị Admin khóa
        // (status = LOCKED/INACTIVE) vẫn đăng nhập được bình thường. Bảng app_users có
        // ràng buộc status IN ('ACTIVE','LOCKED','INACTIVE') nhưng chưa hề được dùng ở đây.
        if (!"ACTIVE".equals(user.getStatus())) {
            model.addAttribute("error", "Tài khoản đã bị khóa hoặc ngừng hoạt động. Vui lòng liên hệ Admin.");
            return "auth/login";
        }

        session.setAttribute("loggedInUser", user);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("loggedInUser");
        return "auth/login";
    }
}