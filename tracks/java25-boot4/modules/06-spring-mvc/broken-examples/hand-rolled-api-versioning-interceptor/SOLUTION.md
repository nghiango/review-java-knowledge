# Solution: Hand-Rolled API Versioning Interceptor

## Issues Identified

```java
package lab.java25boot4.springmvc.broken.apiversioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LegacyVersionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String versionHeader = request.getHeader("X-API-Version");
        // Architecture issue: Hand-rolled interceptor bypasses Spring MVC's HandlerMapping routing, preventing OpenAPI documentation tools and content negotiation from recognizing versioned endpoints
        if (versionHeader == null || (!versionHeader.equals("1.0") && !versionHeader.equals("2.0"))) {
            // API design issue: Returns raw 400 status without RFC 9457 ProblemDetail or negotiated error representations
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        // Architecture issue: Storing version in request attributes couples controllers to low-level Servlet API and manual if-else dispatching
        request.setAttribute("resolvedVersion", versionHeader);
        return true;
    }
}
```

```java
package lab.java25boot4.springmvc.broken.apiversioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    // Architecture issue: Controller method contains manual if-else version dispatching and returns wildcard ResponseEntity<?> instead of strongly-typed domain DTOs
    public ResponseEntity<?> getOrder(@PathVariable String id, HttpServletRequest request) {
        String version = (String) request.getAttribute("resolvedVersion");
        if ("1.0".equals(version)) {
            return ResponseEntity.ok(new OrderV1Response(id, "LEGACY_STATUS"));
        } else if ("2.0".equals(version)) {
            return ResponseEntity.ok(new OrderV2Response(id, "FULFILLED", 9900));
        }
        return ResponseEntity.badRequest().body("Unsupported API version");
    }

    public record OrderV1Response(String orderId, String status) {}

    public record OrderV2Response(String orderId, String status, int amountCents) {}
}
```

### 1. Architecture issue (Bypassing HandlerMapping & In-Controller Dispatching)
Hand-rolled interceptors extract headers and hide versioning from the Spring MVC routing engine. This forces controllers into clumsy `if-else` blocks, destroys OpenAPI schema generation, and prevents Spring MVC from picking distinct method handlers.

### 2. API design issue (Lack of Standard Error Representation)
Rejecting requests via `response.setStatus(SC_BAD_REQUEST)` produces empty error responses, violating modern RFC 9457 `ProblemDetail` guidelines.

## Refactored Solution (Spring Framework 7 Native API Versioning)
In Spring Framework 7 / Spring Boot 4:
1. Versioning is a first-class feature of `@RequestMapping` and handler mapping.
2. Distinct handler methods or controllers declare their supported version cleanly.
3. Unsupported versions automatically trigger standard RFC 9457 `ProblemDetail` 406 Not Acceptable or 400 Bad Request responses.
