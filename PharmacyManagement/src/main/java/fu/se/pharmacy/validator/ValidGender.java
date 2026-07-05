package fu.se.pharmacy.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Kiểm tra giới tính phải là MALE, FEMALE hoặc OTHER (hoặc null/blank).
 */
@Documented
@Constraint(validatedBy = ValidGenderValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGender {
    String message() default "Giới tính không hợp lệ (chọn: MALE, FEMALE hoặc OTHER)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}