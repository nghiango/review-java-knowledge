package lab.springboot.questions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class Q03AutoConfigurationMechanismExample {

    record TokenService(String secret) {}

    @AutoConfiguration
    static class TokenServiceAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TokenService defaultTokenService() {
            return new TokenService("default-auto-configured-secret");
        }
    }

    public static void main(String[] args) {
        // AutoConfiguration classes are discovered from
        // META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        TokenServiceAutoConfiguration autoConfig = new TokenServiceAutoConfiguration();
        TokenService service = autoConfig.defaultTokenService();

        String secret = service.secret(); // "default-auto-configured-secret"
        boolean isAutoConfigured = secret.startsWith("default-"); // true

        System.out.println(
                "Auto-configured token service secret: " + secret + ", auto: " + isAutoConfigured);
    }
}
