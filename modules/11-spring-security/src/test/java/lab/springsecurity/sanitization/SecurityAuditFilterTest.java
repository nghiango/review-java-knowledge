package lab.springsecurity.sanitization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SecurityAuditFilterTest {

    @Test
    @DisplayName("Bearer token is safely masked in audit logs")
    void maskAuthorizationHeader_bearerToken_masksMiddleCharacters() {
        String rawHeader =
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ";
        String masked = SecurityAuditFilter.maskAuthorizationHeader(rawHeader);

        assertThat(masked).startsWith("Bearer eyJh...");
        assertThat(masked).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    @DisplayName("Basic auth header is replaced with protected label")
    void maskAuthorizationHeader_basicAuth_replacesWithProtectedLabel() {
        String rawHeader = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";
        String masked = SecurityAuditFilter.maskAuthorizationHeader(rawHeader);

        assertThat(masked).isEqualTo("Basic [PROTECTED]");
    }

    @Test
    @DisplayName("Sanitized auth controller does not leak password in 401 error message")
    void authenticate_invalidCredentials_doesNotEchoPassword() {
        var controller = new SanitizedAuthController();
        var request = new SanitizedAuthController.AuthRequest("john", "mySecretPassword123");

        ResponseEntity<String> response = controller.authenticate(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid username or password");
        assertThat(response.getBody()).doesNotContain("mySecretPassword123");
    }
}
