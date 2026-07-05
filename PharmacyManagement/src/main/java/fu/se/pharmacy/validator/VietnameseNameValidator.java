package fu.se.pharmacy.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator cho tên người/tổ chức tiếng Việt.
 *
 * Pattern giải thích:
 *  \p{L}  = mọi chữ cái Unicode (bao gồm tiếng Việt có dấu: ă, â, đ, ê, ô, ơ, ư...)
 *  \s     = khoảng trắng
 *  .      = dấu chấm (nếu allowDot = true)
 *  0-9    = chữ số cuối tên (nếu allowTrailingNumbers = true)
 */
public class VietnameseNameValidator
        implements ConstraintValidator<VietnameseName, String> {

    private boolean allowTrailingNumbers;
    private boolean allowDot;

    @Override
    public void initialize(VietnameseName annotation) {
        this.allowTrailingNumbers = annotation.allowTrailingNumbers();
        this.allowDot = annotation.allowDot();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // null / blank được xử lý bởi @NotBlank — ở đây bỏ qua
        if (value == null || value.isBlank()) return true;

        String trimmed = value.trim();

        // Không cho phép khoảng trắng đôi
        if (trimmed.contains("  ")) {
            replaceMessage(ctx, "Tên không được chứa nhiều khoảng trắng liên tiếp");
            return false;
        }

        // Xây dựng pattern động
        StringBuilder allowed = new StringBuilder("[\\p{L}\\s");
        if (allowDot)             allowed.append(".");
        if (allowTrailingNumbers) allowed.append("0-9");
        allowed.append("]+");

        if (!trimmed.matches(allowed.toString())) {
            replaceMessage(ctx, buildMessage());
            return false;
        }

        // Phải bắt đầu bằng chữ cái (không bắt đầu bằng số hay khoảng trắng)
        if (!Character.isLetter(trimmed.charAt(0))) {
            replaceMessage(ctx, "Tên phải bắt đầu bằng chữ cái");
            return false;
        }

        return true;
    }

    private String buildMessage() {
        if (allowDot && allowTrailingNumbers)
            return "Tên chỉ được chứa chữ cái, khoảng trắng, dấu chấm và chữ số";
        if (allowDot)
            return "Tên chỉ được chứa chữ cái, khoảng trắng và dấu chấm";
        if (allowTrailingNumbers)
            return "Tên chỉ được chứa chữ cái, khoảng trắng và chữ số";
        return "Tên chỉ được chứa chữ cái và khoảng trắng";
    }

    private void replaceMessage(ConstraintValidatorContext ctx, String msg) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
    }
}