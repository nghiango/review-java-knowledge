package lab.springsecurity.broken.csrf;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class InsecureWebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF disabled on session-cookie authenticated application!
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(insecureCorsSource()))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/api/account/balance"))
                .build();
    }

    @Bean
    public CorsConfigurationSource insecureCorsSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Dangerous combination: Wildcard origins with allowCredentials=true
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
