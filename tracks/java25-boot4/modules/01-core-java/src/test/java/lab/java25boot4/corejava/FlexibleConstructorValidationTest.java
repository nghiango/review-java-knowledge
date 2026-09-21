package lab.java25boot4.corejava;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlexibleConstructorValidationTest {

    @Test
    @DisplayName("Flexible constructor validates and normalizes arguments prior to super(...)")
    void shouldValidateAndNormalizeBeforeSuper() {
        var user =
                new FlexibleConstructorValidation.UserAccount("  USR-101  ", "ALICE@EXAMPLE.COM");

        assertThat(user.getId()).isEqualTo("usr-101");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Flexible constructor throws fast on invalid arguments before super invocation")
    void shouldFailFastOnInvalidArguments() {
        assertThatThrownBy(
                        () ->
                                new FlexibleConstructorValidation.UserAccount(
                                        "", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Normalized ID cannot be empty");

        assertThatThrownBy(
                        () ->
                                new FlexibleConstructorValidation.UserAccount(
                                        "USR-102", "invalid-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }
}
