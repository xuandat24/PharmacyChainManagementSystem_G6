package fu.se.pharmacy.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneValidator implements ConstraintValidator<ValidPhone, String> {

    // 10 chữ số bắt đầu 0, hoặc +84 + 9 chữ số
    private static final String PHONE_REGEX = "^(0[3-9][0-9]{8}|\\+84[3-9][0-9]{8})$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // Số điện thoại không bắt buộc — null/blank = hợp lệ
        if (value == null || value.isBlank()) return true;

        String cleaned = value.trim().replaceAll("[\\s\\-]", "");

        if (!cleaned.matches(PHONE_REGEX)) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    "Số điện thoại không hợp lệ. Phải là 10 chữ số bắt đầu bằng 0 (VD: 0912345678)"
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}