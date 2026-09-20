# Solution: CORS Wildcard with Credentials

## Annotated Code

### `WebCorsConfig.java`

```java
package lab.springmvc.broken.corssecurity;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Security issue: Wildcard origin '*' combined with allowCredentials(true) violates W3C CORS specifications
        // Security issue: Exposes authenticated user sessions and cookies to malicious third-party websites
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowCredentials(true);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `WebCorsConfig.java:14` | `Security` | `allowedOrigins("*")` with `allowCredentials(true)` | Violates W3C CORS specification; modern browsers reject responses with `Access-Control-Allow-Origin: *` when `Access-Control-Allow-Credentials: true`. |
| `AccountController.java:9` | `Security` | Insecure `@CrossOrigin` on sensitive endpoint | Permits cross-origin exfiltration of authenticated profile data if improperly relaxed. |

## Correct implementation

- Package: `lab.springmvc.corssecurity`
- Production reference: `WebCorsConfig.java`, `AccountController.java`
- Fix: Use explicit origin lists or `allowedOriginPatterns("https://*.company.com", "https://app.company.com")` when credentials are required (`allowCredentials(true)`). Never use wildcard `*` with credential sharing.
