package fu.se.pharmacy.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Kiểm tra ngày không được ở tương lai (dùng cho ngày sinh, ngày kê đơn...).
 * Hoặc kiểm tra ngày phải sau ngày hiện tại (dùng cho ngày hết hạn).
 */
@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "Ngày không hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** Nếu true: ngày phải <= hôm nay (ngày sinh, ngày kê đơn) */
    boolean pastOrPresent() default false;

    /** Nếu true: ngày phải >= hôm nay (ngày hết hạn đơn thuốc) */
    boolean futureOrPresent() default false;

    /** Năm tối thiểu (mặc định 1900) */
    int minYear() default 1900;
}