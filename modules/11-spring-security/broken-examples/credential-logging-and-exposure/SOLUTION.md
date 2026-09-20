# Solution: Credential Logging & Sensitive Data Exposure

## Annotated Code

### `SecurityLoggingFilter.java`
```java
package lab.springsecurity.broken.sanitization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(SecurityLoggingFilter.class.getName());

    // Security issue: Logging unmasked Authorization headers (Bearer JWT tokens, Basic auth base64 credentials, API keys) writes secrets to persistent log files and observability platforms (CWE-532).
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            LOGGER.info("Incoming request to " + request.getRequestURI() + " with Authorization: " + authHeader);
        }

        filterChain.doFilter(request, response);
    }
}
```

### `AuthenticationController.java`
```java
package lab.springsecurity.broken.sanitization;

import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());

    // Security issue: Plaintext password logged directly to application output streams (CWE-532).
    // Security issue: Password reflected back in HTTP 401 response body, leaking credentials to proxies and client-side loggers (CWE-209).
    @PostMapping("/token")
    public ResponseEntity<String> authenticate(@RequestBody AuthRequest request) {
        LOGGER.info("Authenticating user: " + request.username() + " with password: " + request.password());

        if ("admin".equals(request.username()) && "secret123".equals(request.password())) {
            return ResponseEntity.ok("token_eyJhbGciOi...");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials for: " + request.username() + "/" + request.password());
    }

    public record AuthRequest(String username, String password) {}
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Authorization Header & Token Exposure in Logs | Security | Critical | Logging raw `Authorization: Bearer <token>` or `Basic <base64>` headers commits sensitive session credentials to server logs, SIEM systems, and centralized log aggregators where unauthorized operators can harvest them (CWE-532). |
| Plaintext Password Logging | Security | Critical | Writing passwords to application logger streams violates compliance standards (PCI-DSS §3.4, GDPR, SOC 2) and exposes credentials to log pipeline breaches. |
| Password Reflection in Error Responses | Security | Major | Echoing supplied passwords back in error messages exposes credentials across proxy caches, browser histories, and network sniffers (CWE-209). |

## Correct Implementation Reference
- [`SecurityAuditFilter.java`](../../src/main/java/lab/springsecurity/sanitization/SecurityAuditFilter.java)
- [`SanitizedAuthController.java`](../../src/main/java/lab/springsecurity/sanitization/SanitizedAuthController.java)
