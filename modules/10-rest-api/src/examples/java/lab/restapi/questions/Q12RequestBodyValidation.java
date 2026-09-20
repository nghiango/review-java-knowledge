package lab.restapi.questions;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public class Q12RequestBodyValidation {

    public record UserDto(
            @NotBlank(message = "Username must not be blank") String username,
            @Email(message = "Invalid email format") String email) {}

    public static void main(String[] args) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // Valid payload
        UserDto validUser = new UserDto("johndoe", "john@example.com");
        Set<?> violationsEmpty = validator.validate(validUser);
        boolean isValid = violationsEmpty.isEmpty(); // true

        // Invalid payload
        UserDto invalidUser = new UserDto("", "not-an-email");
        var violations = validator.validate(invalidUser);
        int violationCount = violations.size(); // 2

        System.out.println(
                "Valid user is valid: "
                        + isValid
                        + ", violations: "
                        + violationsEmpty.size()); // Valid user is valid: true, violations: 0
        System.out.println(
                "Invalid user violations count: "
                        + violationCount); // Invalid user violations count: 2
    }
}
