package fu.se.pharmacy.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Kiểm tra chuỗi có phải tên người hợp lệ không.
 * Chấp nhận: chữ cái tiếng Việt có dấu, khoảng trắng, dấu chấm (BS., CN1...)
 * Không chấp nhận: số, ký tự đặc biệt như @#$%^&*
 */
@Documented
@Constraint(validatedBy = VietnameseNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VietnameseName {
    String message() default "Tên chỉ được chứa chữ cái và khoảng trắng";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** Cho phép có số ở cuối tên (VD: "Quản CN1", "Dược Sĩ 1") */
    boolean allowTrailingNumbers() default false;

    /** Cho phép dấu chấm (VD: "BS. Nguyễn") */
    boolean allowDot() default false;
}