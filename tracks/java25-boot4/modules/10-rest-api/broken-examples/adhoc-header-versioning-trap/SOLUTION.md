# Solution: Ad-Hoc Header Versioning & Missing Lifecycle Headers

## Annotated Code

```java
package lab.java25boot4.restapi.broken.adhocversioning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v-orders")
public class OrderResourceController {

    @GetMapping("/{id}")
    // Maintainability issue: manual version branching in controller methods duplicates endpoint logic instead of using native Spring 7 version routing
    public ResponseEntity<?> fetchOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-API-Version", required = false) String version) {
        if (version == null || version.equals("1")) {
            // Observability issue: deprecated API versions do not emit RFC 8594 Sunset or Deprecation headers
            LegacyOrderDto dto = new LegacyOrderDto(id, "CUST-100", 150.0, "PROCESSING");
            return ResponseEntity.ok(dto);
        } else if (version.equals("2")) {
            ModernOrderRepresentation rep =
                    new ModernOrderRepresentation(id, "CUST-100", 15000L, "PROCESSING", "USD");
            return ResponseEntity.ok(rep);
        } else {
            // Reliability issue: ad-hoc header version check throws unhandled exception or returns 500 instead of RFC 9457 ProblemDetail for unsupported versions
            throw new IllegalArgumentException("Unsupported API version requested: " + version);
        }
    }

    public record ModernOrderRepresentation(
            String orderId,
            String customerId,
            long amountCents,
            String status,
            String currency) {}
}
```

---

## Issues Identified

### 1. Manual Version Branching Polluting Controller Methods
- **Category:** Maintainability
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Method signatures accept raw headers and use imperative `if/else` checks to construct different representations. This tightly couples multiple API schema versions into a single method, bloats cyclomatic complexity, and prevents declarative route generation in OpenAPI.
- **Remediation:** Leverage Spring Framework 7 / Spring Boot 4 native version routing (`@RequestMapping` with version attributes or dedicated versioned controllers with version resolvers), cleanly separating v1 and v2 handlers.

### 2. Missing RFC 8594 Deprecation and Sunset Headers
- **Category:** Observability
- **Track:** `java25-boot4`
- **Severity:** Medium
- **Description:** Serving v1 representations without `Deprecation: @<timestamp>` or `Sunset: <date>` HTTP headers leaves API clients blind to forthcoming deprecation and shutdown deadlines.
- **Remediation:** Attach standardized RFC 8594 `Sunset` and `Deprecation` response headers when serving superseded API versions to enable client telemetry and automated migration alerts.

### 3. Unhandled Invalid Version Yielding HTTP 500
- **Category:** Reliability
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Throwing `IllegalArgumentException` results in a default HTTP 500 Internal Server Error (or inconsistent servlet container error page) instead of a structured client error (`400 Bad Request` or `406 Not Acceptable`).
- **Remediation:** Use RFC 9457 `ProblemDetail` with status `400 Bad Request` or `406 Not Acceptable` explicitly explaining supported API versions and detailing available upgrade pathways.
