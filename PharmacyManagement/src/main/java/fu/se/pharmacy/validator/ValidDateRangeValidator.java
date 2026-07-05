package fu.se.pharmacy.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, LocalDate> {

    private boolean pastOrPresent;
    private boolean futureOrPresent;
    private int minYear;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.pastOrPresent  = annotation.pastOrPresent();
        this.futureOrPresent = annotation.futureOrPresent();
        this.minYear        = annotation.minYear();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // null được xử lý bởi @NotNull riêng

        LocalDate today = LocalDate.now();

        if (value.getYear() < minYear) {
            fail(ctx, "Năm không được nhỏ hơn " + minYear);
            return false;
        }

        if (pastOrPresent && value.isAfter(today)) {
            fail(ctx, "Ngày không được ở tương lai");
            return false;
        }

        if (futureOrPresent && value.isBefore(today)) {
            fail(ctx, "Ngày đã qua, vui lòng kiểm tra lại");
            return false;
        }

        return true;
    }

    private void fail(ConstraintValidatorContext ctx, String msg) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
    }
}