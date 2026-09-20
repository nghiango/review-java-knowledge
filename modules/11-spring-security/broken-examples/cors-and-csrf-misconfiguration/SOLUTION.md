# Solution: CORS & CSRF Misconfiguration

## Annotated Code

### `InsecureWebSecurityConfig.java`
```java
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

    // Security issue: Disabling CSRF protection on an application utilizing stateful cookie-based session authentication (formLogin / JSESSIONID).
    // Security issue: An attacker site can trick authenticated browser sessions into submitting unwanted POST requests (e.g. fund transfers) via cross-site forgery.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(insecureCorsSource()))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/api/account/balance"))
                .build();
    }

    // Security issue: Combining allowedOriginPatterns("*") with allowCredentials(true) allows any malicious website to read authenticated response data through XMLHttpRequest / fetch.
    @Bean
    public CorsConfigurationSource insecureCorsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Disabling CSRF on Stateful Cookie Sessions | Security | Critical | Disabling CSRF is only safe for purely stateless bearer-token APIs. For applications using browser session cookies (`JSESSIONID`), disabling CSRF allows malicious third-party websites to trigger authenticated mutating actions on behalf of logged-in victims (CWE-352). |
| Wildcard CORS Origins with Credentials Enabled | Security | Critical | Configuring `allowedOriginPatterns("*")` alongside `allowCredentials(true)` allows any domain in the world to execute credentialed cross-origin HTTP requests and read response bodies, effectively neutralizing the Same-Origin Policy (CWE-942). |

## Correct Implementation Reference
- [`SafeSessionSecurityConfig.java`](../../src/main/java/lab/springsecurity/csrf/SafeSessionSecurityConfig.java)
- [`SafeStatelessSecurityConfig.java`](../../src/main/java/lab/springsecurity/csrf/SafeStatelessSecurityConfig.java)
