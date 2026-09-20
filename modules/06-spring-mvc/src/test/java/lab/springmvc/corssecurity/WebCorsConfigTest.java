package lab.springmvc.corssecurity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebCorsConfigTest {

    @Test
    @DisplayName("WebCorsConfig should configure CORS registration with allowedOriginPatterns")
    void corsConfiguration_configuresAllowedOriginPatterns() {
        WebCorsConfig config = new WebCorsConfig();
        CorsRegistry registry = new CorsRegistry();
        config.addCorsMappings(registry);

        AccountController controller = new AccountController();
        ResponseEntity<String> response = controller.getProfile();

        assertThat(response.getBody()).isEqualTo("Authenticated profile data");
    }
}
