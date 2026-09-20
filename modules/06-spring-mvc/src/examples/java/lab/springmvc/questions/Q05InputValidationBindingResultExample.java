package lab.springmvc.questions;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public class Q05InputValidationBindingResultExample {

    record ItemRequest(@NotBlank String name, @Min(1) int quantity) {}

    public static void main(String[] args) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        ItemRequest valid = new ItemRequest("Book", 2);
        Set<?> validErrors = validator.validate(valid);
        boolean isValidEmpty = validErrors.isEmpty(); // true

        ItemRequest invalid = new ItemRequest("", 0);
        Set<?> invalidErrors = validator.validate(invalid);
        int errorCount = invalidErrors.size(); // 2

        System.out.println(
                "Valid request errors: " + isValidEmpty + ", invalid errors count: " + errorCount);
    }
}
