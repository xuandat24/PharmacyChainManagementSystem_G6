package fu.se.pharmacy.config;

import fu.se.pharmacy.entity.Employee;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Employee user = (Employee) session.getAttribute("loggedInUser");

        // 1. Kiểm tra đã đăng nhập chưa
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 2. Lấy đường dẫn mà người dùng đang cố truy cập
        String uri = request.getRequestURI();

        // 3. Phân quyền: Nếu KHÔNG PHẢI Admin (roleId != 1) mà cố tình vào các trang nhạy cảm
        if (user.getRoleId() != 1) {
            if (uri.startsWith("/employees") || uri.startsWith("/branches") || uri.startsWith("/settings")) {
                // Đá văng về trang chủ
                response.sendRedirect("/");
                return false;
            }
        }

        return true;
    }
}