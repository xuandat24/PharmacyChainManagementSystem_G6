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

    /**
     * Kiểm tra đã đăng nhập chưa, trả về AppUser để controller dùng luôn.
     * Nếu chưa login → throw AccessDeniedException → trang 403.
     */
    public static AppUser requireLogin(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new GlobalExceptionHandler.AccessDeniedException();
        }
        return user;
    }

    /**
     * Kiểm tra role, trả về AppUser để controller dùng luôn.
     * Nếu không đúng role → throw AccessDeniedException → trang 403.
     *
     * Cách dùng:
     *   AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
     */
    public static AppUser requireRole(HttpSession session, String... allowedRoles) {
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new GlobalExceptionHandler.AccessDeniedException();
        }
        String role = user.getRole();
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) return user;
        }
        throw new GlobalExceptionHandler.AccessDeniedException();
    }
}
