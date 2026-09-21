# Solution: Legacy Spring Security DSL Chaining & Matcher Ordering

## Annotated Code

```java
package lab.java25boot4.springsecurity.broken.chainingdsl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class LegacySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Maintainability issue: legacy and() chaining and non-lambda configuration removed in Spring Security 7
                .authorizeHttpRequests()
                .requestMatchers("/public/**").permitAll()
                // Security issue: broad permitAll pattern or misplaced request matcher leaks protected admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()
                .csrf().disable();

        return http.build();
    }
}
```

---

## Issues Identified

### 1. Obsolete `and()` Chaining and Non-Lambda Configuration
- **Category:** Maintainability
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Spring Security 7 completely removes `and()` method chaining and parameterless configuration methods (like `authorizeHttpRequests()` and `csrf()`). All configurations must use lambda customizers (e.g. `authorizeHttpRequests(auth -> auth...)`).
- **Remediation:** Refactor the configuration to use the lambda DSL: `http.authorizeHttpRequests(auth -> auth.requestMatchers(...)...).csrf(AbstractHttpConfigurer::disable)`.

### 2. Broad `anyRequest().permitAll()` Authorization Hole
- **Category:** Security
- **Track:** `java25-boot4`
- **Severity:** Critical
- **Description:** Defaulting unmatched endpoints to `permitAll()` creates severe vulnerability surface area when new controllers or actuator endpoints are introduced, as they will default to unauthenticated access rather than secure-by-default.
- **Remediation:** Always adopt secure-by-default semantics by terminating request authorization chains with `.anyRequest().authenticated()`.
