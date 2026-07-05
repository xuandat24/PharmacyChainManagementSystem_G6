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
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Vui long dang nhap\"}");
                return false;
            }
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }

    /**
     * Kiem tra da dang nhap chua, tra ve AppUser de controller dung luon.
     * Neu chua login -> throw AccessDeniedException -> trang 403.
     */
    public static AppUser requireLogin(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new GlobalExceptionHandler.AccessDeniedException();
        }
        return user;
    }

    /**
     * Kiem tra role, tra ve AppUser de controller dung luon.
     * Neu khong dung role -> throw AccessDeniedException -> trang 403.
     *
     * Cach dung:
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
