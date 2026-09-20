# Solution: Inconsistent Error Contract and Stack Trace Leakage

## Annotated Code

### `LegacyErrorHandler.java`
```java
package lab.restapi.broken.problemdetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LegacyErrorHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        // Security issue: Leaking internal exception classes, database queries, and server stack traces enables attacker reconnaissance (OWASP API8:2023)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        Map.of(
                                "exception", ex.getClass().getName(),
                                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                                "stackTrace", sw.toString()));
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Stack Trace and Internal Exception Leakage | High | Security | Emitting internal Java stack traces and package hierarchies exposes infrastructure details and SQL queries to potential attackers. |
| Inconsistent Error Shapes | Medium | Maintainability | Returning plain text strings, differing JSON keys (`errorMessage` vs `errors`), and varied data types prevents API clients from implementing structured error handling. |

---

## Remediation Strategy: Standardize with RFC 9457 `ProblemDetail`

Spring Boot 3 / Spring Framework 6 native `ProblemDetail` standardizes machine-readable error responses with `Content-Type: application/problem+json`:

```json
{
  "type": "https://api.example.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Customer with ID 999 was not found",
  "instance": "/api/customers/999",
  "code": "CUSTOMER_NOT_FOUND",
  "timestamp": "2026-09-20T12:00:00Z"
}
```

Implement central handling via `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`.
