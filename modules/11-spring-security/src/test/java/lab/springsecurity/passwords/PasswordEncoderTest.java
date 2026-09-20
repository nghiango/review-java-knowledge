package lab.springsecurity.passwords;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;
    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        registrationService = new UserRegistrationService(passwordEncoder);
    }

    @Test
    @DisplayName("Password is saved with bcrypt algorithm prefix and salt")
    void registerUser_validPassword_encodesWithDelegatingFormat() {
        UserAccount account =
                registrationService.registerUser("john_doe", "SuperSecurePassword123");

        assertThat(account.passwordHash()).startsWith("{bcrypt}");
        assertThat(account.passwordHash()).isNotEqualTo("SuperSecurePassword123");
    }

    @Test
    @DisplayName("Authentication succeeds with correct password and fails with incorrect")
    void authenticate_correctAndIncorrectPassword_returnsExpectedResults() {
        registrationService.registerUser("jane_doe", "SecretPassphrase99");

        boolean authenticatedSuccess =
                registrationService.authenticate("jane_doe", "SecretPassphrase99");
        boolean authenticatedFailure =
                registrationService.authenticate("jane_doe", "WrongPassphrase");

        assertThat(authenticatedSuccess).isTrue();
        assertThat(authenticatedFailure).isFalse();
    }

    @Test
    @DisplayName(
            "Registering with password shorter than 8 characters throws IllegalArgumentException")
    void registerUser_shortPassword_throwsException() {
        assertThatThrownBy(() -> registrationService.registerUser("alice", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }
}
