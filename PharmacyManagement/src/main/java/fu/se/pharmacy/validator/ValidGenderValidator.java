package fu.se.pharmacy.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ValidGenderValidator implements ConstraintValidator<ValidGender, String> {

    private static final Set<String> ALLOWED = Set.of("MALE", "FEMALE", "OTHER");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // Giới tính không bắt buộc
        if (value == null || value.isBlank()) return true;
        return ALLOWED.contains(value.toUpperCase());
    }
}