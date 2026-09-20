# Solution: Leaking Internal Exceptions and Stack Traces

## Annotated Code

### `GlobalExceptionHandler.java`

```java
package lab.springmvc.broken.exceptionhandling;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        Map<String, Object> errorBody = new HashMap<>();
        // Security issue: Raw exception message exposes internal SQL query structure and database schema names
        errorBody.put("error", ex.getMessage());
        // Security issue: Full class name leaks internal architecture, framework versions, and package structure
        errorBody.put("exceptionClass", ex.getClass().getName());
        // Security issue: Complete stack trace exposes internal code paths, third-party libraries, and line numbers to attackers
        errorBody.put("stackTrace", Arrays.toString(ex.getStackTrace()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `GlobalExceptionHandler.java:18` | `Security` | Leaking raw exception message | May disclose database connection strings, table names, and internal parameters. |
| `GlobalExceptionHandler.java:20` | `Security` | Exposing internal class names | Gives attackers intelligence about internal frameworks and component names. |
| `GlobalExceptionHandler.java:22` | `Security` | Exposing raw stack trace in HTTP payload | Critical information disclosure (CWE-209); aids attackers in vulnerability exploitation. |

## Correct implementation

- Package: `lab.springmvc.exceptionhandling`
- Production reference: `OrderNotFoundException.java`, `GlobalExceptionHandler.java`
- Fix: Implement `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` or returning RFC 9457 `ProblemDetail`. Sanitize public error messages, log full stack traces internally with correlation IDs, and return generic descriptions for unexpected 500 errors.
