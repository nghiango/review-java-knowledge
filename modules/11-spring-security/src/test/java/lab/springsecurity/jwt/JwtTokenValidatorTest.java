package lab.springsecurity.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;

    @BeforeEach
    void setUp() {
        validator =
                new JwtTokenValidator(
                        "super-secret-production-hmac-key-minimum-256-bits-length-required-12345");
    }

    @Test
    @DisplayName(
            "Valid JWT with matching HMAC signature and active timestamp extracts claims successfully")
    void validateAndExtract_validToken_succeeds() {
        String token =
                validator.createToken("user-123", "user@example.com", List.of("ROLE_USER"), 3600);

        JwtClaims claims = validator.validateAndExtract(token);

        assertThat(claims.subject()).isEqualTo("user-123");
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.roles()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Expired JWT throws CredentialsExpiredException")
    void validateAndExtract_expiredToken_throwsCredentialsExpiredException() {
        String expiredToken =
                validator.createToken("user-123", "user@example.com", List.of("ROLE_USER"), -10);

        assertThatThrownBy(() -> validator.validateAndExtract(expiredToken))
                .isInstanceOf(CredentialsExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Tampered JWT payload or signature throws BadCredentialsException")
    void validateAndExtract_tamperedToken_throwsBadCredentialsException() {
        String token =
                validator.createToken("user-123", "user@example.com", List.of("ROLE_USER"), 3600);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> validator.validateAndExtract(tamperedToken))
                .isInstanceOf(BadCredentialsException.class);
    }
}
