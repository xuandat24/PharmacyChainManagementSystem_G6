package fu.se.pharmacy.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Kiểm tra số điện thoại Việt Nam hợp lệ.
 * Chấp nhận: 10 chữ số, bắt đầu bằng 0
 * Hoặc: +84xxxxxxxxx (11 ký tự)
 */
@Documented
@Constraint(validatedBy = ValidPhoneValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Số điện thoại không hợp lệ (VD: 0912345678)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}