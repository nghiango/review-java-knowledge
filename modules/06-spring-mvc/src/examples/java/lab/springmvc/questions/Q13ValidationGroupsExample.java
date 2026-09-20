package lab.springmvc.questions;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.Set;

public class Q13ValidationGroupsExample {

    interface OnCreate {}

    interface OnUpdate {}

    record AccountCommand(
            @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) String id,
            @NotNull String username) {}

    public static void main(String[] args) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // During Create, ID must be null
        AccountCommand createPayload = new AccountCommand("acc-123", "alice");
        Set<?> createViolations = validator.validate(createPayload, OnCreate.class);
        boolean hasCreateViolation =
                !createViolations.isEmpty(); // true (id must be null on create)

        // During Update, ID must NOT be null
        AccountCommand updatePayload = new AccountCommand("acc-123", "alice");
        Set<?> updateViolations = validator.validate(updatePayload, OnUpdate.class);
        boolean isUpdateValid = updateViolations.isEmpty(); // true

        System.out.println(
                "Create violation on existing ID: "
                        + hasCreateViolation
                        + ", Update valid: "
                        + isUpdateValid);
    }
}
