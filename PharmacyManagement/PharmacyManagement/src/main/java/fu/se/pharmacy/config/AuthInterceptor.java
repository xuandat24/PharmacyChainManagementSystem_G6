package fu.se.pharmacy.config;

import fu.se.pharmacy.entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession();

        if (session.getAttribute("loggedInUser") == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }

    // ===== Static helper: dùng trong controllers để kiểm tra role =====

    /**
     * Kiểm tra user có đúng role không.
     * Nếu không → throw AccessDeniedException → GlobalExceptionHandler trả về trang 403.
     *
     * Cách dùng trong controller:
     *   AuthInterceptor.requireRole(session, "Admin", "BranchManager");
     */
    public static void requireRole(HttpSession session, String... allowedRoles) {
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new GlobalExceptionHandler.AccessDeniedException();
        }
        String role = user.getRole();
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) return;
        }
        throw new GlobalExceptionHandler.AccessDeniedException();
    }

    /**
     * Kiểm tra user đã đăng nhập chưa (không quan tâm role).
     */
    public static AppUser requireLogin(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new GlobalExceptionHandler.AccessDeniedException();
        }
        return user;
    }
}