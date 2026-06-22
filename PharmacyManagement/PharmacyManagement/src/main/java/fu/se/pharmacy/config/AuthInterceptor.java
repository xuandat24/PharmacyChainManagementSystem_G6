package fu.se.pharmacy.config;

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

        // Kiểm tra xem đã có thông tin user đăng nhập chưa
        if (session.getAttribute("loggedInUser") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}