package fu.se.pharmacy.config;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Xử lý lỗi toàn cục.
 * Khi controller throw AccessDeniedException → hiển thị trang error/403.html
 * thay vì Whitelabel Error Page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Model model) {
        return "error/403";
    }

    /**
     * Exception dùng trong controllers để chặn truy cập không hợp lệ.
     * Thay vì return "redirect:/..." → throw new AccessDeniedException()
     * → hiện trang "Bạn không có quyền hạn truy cập".
     */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException() {
            super("Bạn không có quyền hạn truy cập");
        }
    }
}