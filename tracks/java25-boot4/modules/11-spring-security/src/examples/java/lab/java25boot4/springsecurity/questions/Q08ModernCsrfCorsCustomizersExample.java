package lab.java25boot4.springsecurity.questions;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;

/**
 * Q08: How are CSRF and CORS configured cleanly in Spring Security 7 using modern Customizer
 * functions?
 */
public class Q08ModernCsrfCorsCustomizersExample {

    public static void main(String[] args) {
        Customizer<CsrfConfigurer<HttpSecurity>> disableCsrf = AbstractHttpConfigurer::disable;

        boolean isCustomizerPresent = (disableCsrf != null);

        System.out.println("Customizer non-null: " + isCustomizerPresent); // true
        System.out.println("Stateless API standard: CSRF disabled for token-based APIs"); // true
    }
}
