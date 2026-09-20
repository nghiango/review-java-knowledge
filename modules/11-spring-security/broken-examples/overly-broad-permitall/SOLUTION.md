# Solution: Overly Broad PermitAll & RequestMatcher Ordering

## Annotated Code

### `InsecureSecurityConfig.java`
```java
package lab.springsecurity.broken.filterchain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class InsecureSecurityConfig {

    // Security issue: Request matcher order vulnerability. In Spring Security, authorizeHttpRequests matches rules in top-down order (first match wins).
    // Security issue: Declaring '/api/**' as permitAll() before '/api/admin/**' matches all admin requests first, completely bypassing role checks.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .build();
    }
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| RequestMatcher Top-Down Ordering Bypass | Security | Critical | Spring Security evaluates `authorizeHttpRequests` sequentially: the first matching pattern applies immediately. Placing generic patterns (`/api/**`) before specific restricted patterns (`/api/admin/**`) grants anonymous access to administrative endpoints (CWE-285). |
| Overly Permissive Wildcard Rules | Security | Major | Wildcards like `/**` or `/api/**` in `permitAll()` accidentally expose newly added microservice routes and internal actuator endpoints unless strictly scoped. |

## Correct Implementation Reference
- [`SafeSecurityFilterChainConfig.java`](../../src/main/java/lab/springsecurity/filterchain/SafeSecurityFilterChainConfig.java)
- [`AdminDashboardController.java`](../../src/main/java/lab/springsecurity/filterchain/AdminDashboardController.java)
