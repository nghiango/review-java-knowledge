package lab.springmvc.inputvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserControllerValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Valid create user request should have zero constraint violations")
    void validRequest_passesValidation() {
        CreateUserRequest request = new CreateUserRequest("john_doe", "john@example.com", 150.0);
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();

        UserService service = new UserService();
        UserResponse response = service.registerUser(request);
        assertThat(response.username()).isEqualTo("john_doe");
        assertThat(response.balance()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Blank username should violate @NotBlank and @Size constraints")
    void blankUsername_failsValidation() {
        CreateUserRequest request = new CreateUserRequest("", "john@example.com", 100.0);
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Invalid email should violate @Email constraint")
    void invalidEmail_failsValidation() {
        CreateUserRequest request = new CreateUserRequest("valid_user", "not-an-email", 100.0);
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Negative initial balance should violate @PositiveOrZero constraint")
    void negativeBalance_failsValidation() {
        CreateUserRequest request = new CreateUserRequest("valid_user", "john@example.com", -50.0);
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("initialBalance"));
    }
}
