package lab.java25boot4.springmvc.questions;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Q08ValidationJSpecifyCoexistenceExample {

    // JSpecify provides static compiler/IDE contracts; Bean Validation provides runtime payload
    // constraints
    public record RegistrationForm(@NotNull String username, @NotNull String email) {}

    public static void main(String[] args) {
        var form = new RegistrationForm("alice", "alice@example.com");
        System.out.println(form.username()); // alice
        System.out.println(form.email()); // alice@example.com
    }
}
