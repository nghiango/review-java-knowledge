# Code Review Target: Hand-Rolled API Versioning Interceptor

## Overview
A backend service implements API versioning using a custom `HandlerInterceptor` (`LegacyVersionInterceptor`) that inspects headers, validates the version, and stores it in request attributes. `OrderController` branches on the attribute using manual `if-else` blocks and returns raw `ResponseEntity<?>`.

## Code Under Review
Review `LegacyVersionInterceptor.java` and `OrderController.java`.

## Questions for the Reviewer
1. What happens to OpenAPI/Swagger documentation, content negotiation, and handler mapping routing when version branching is done manually inside the controller body?
2. What happens if a client requests an unmapped version? Does the response conform to RFC 9457 `ProblemDetail`?
3. In Spring Framework 7 / Spring Boot 4, how does first-class framework API versioning eliminate custom interceptors and request attribute passing?
