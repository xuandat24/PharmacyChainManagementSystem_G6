package fu.se.pharmacy.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

// LƯU Ý: đổi 3 import dưới đây cho khớp với package thật của bạn.
// Nếu bạn đã merge theo cấu trúc fu.se.pharmacy.flow4.* (sub-package của
// Người 2/3) thì dùng dòng đã bật sẵn. Nếu bạn vẫn giữ package gốc
// fu.se2033.pharmacy thì đổi lại 3 dòng này tương ứng.
import fu.se.pharmacy.exception.ErrorResponse;
import fu.se.pharmacy.exception.ResourceNotFoundException;
import fu.se.pharmacy.exception.BusinessException;

/**
 * Xử lý lỗi toàn cục cho TOÀN BỘ ứng dụng (cả phần Thymeleaf của Người 2/3
 * lẫn phần REST API của Người 4).
 *
 * Dùng @ControllerAdvice (không phải @RestControllerAdvice) vì ứng dụng có cả
 * controller trả về view name (String) lẫn controller REST trả JSON. Mỗi
 * @ExceptionHandler tự quyết định trả HTML (String view) hay JSON
 * (ResponseEntity + @ResponseBody) dựa vào việc request có phải gọi tới
 * "/api/**" hay không.
 *
 * GỘP TỪ 2 FILE:
 * - fu.se.pharmacy.config.GlobalExceptionHandler (bắt AccessDeniedException → trang 403)
 * - fu.se2033.pharmacy.exception.GlobalExceptionHandler (bắt lỗi REST API → JSON)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // PHẦN THYMELEAF (Người 2/3) — trả về view name
    // =========================================================

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

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(403, "ACCESS_DENIED", "Bạn không có quyền hạn truy cập", null));
        }
        return "error/403";
    }

    // =========================================================
    // PHẦN REST API (Người 4) — trả về JSON
    // =========================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "BUSINESS_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "INVALID_JSON", "Dữ liệu gửi lên không đúng định dạng hoặc sai kiểu dữ liệu", null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getName(), "Giá trị không đúng kiểu dữ liệu");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "TYPE_MISMATCH", "Tham số không hợp lệ", fieldErrors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getParameterName(), "Thiếu tham số bắt buộc");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "MISSING_PARAMETER", "Thiếu dữ liệu đầu vào", fieldErrors));
    }

    // =========================================================
    // FALLBACK CHUNG — lỗi hệ thống không xác định
    // =========================================================

    /**
     * FIX: handleSystem (bắt mọi Exception còn lại) PHẢI phân biệt API vs page,
     * nếu không sẽ luôn trả JSON cho mọi lỗi 500 kể cả lỗi xảy ra ở trang
     * Thymeleaf — khiến người dùng web thấy JSON thô thay vì trang lỗi HTML.
     */
    @ExceptionHandler(Exception.class)
    public Object handleSystem(Exception ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "SYSTEM_ERROR", "Lỗi hệ thống, vui lòng kiểm tra lại", null));
        }
        // Để Spring Boot fallback xử lý lỗi 500 mặc định cho trang Thymeleaf
        // (whitelabel error page hoặc error/500.html nếu có), tránh nuốt mất
        // stack trace hữu ích khi debug trong môi trường dev.
        throw new RuntimeException(ex);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }
}