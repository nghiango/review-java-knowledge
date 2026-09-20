# Spring MVC Tests

## Spring MVC Testing Strategy

Testing Spring MVC applications combines fast unit tests for business logic with focused standalone controller tests:

1. **Pure Unit Testing with Mocks**: Verifies controller routing, service orchestration, pricing rules, and async completion using pure JUnit 5, AssertJ, and Mockito without starting a web container.
2. **Bean Validation Testing**: Verifies that input DTOs enforce Jakarta Validation constraints (`@NotBlank`, `@Email`, `@Min`, `@Size`) before reaching business logic.
3. **Problem Details Verification**: Verifies that custom exception handlers generate RFC 9457 compliant `ProblemDetail` payloads with sanitized error messages and accurate HTTP status codes.
4. **Asynchronous Non-Blocking Verification**: Verifies `CompletableFuture` responses and worker thread offloading under concurrent execution.

## Test Suite Overview

```bash
./gradlew :modules:06-spring-mvc:test
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `UserControllerValidationTest` | Validation constraints enforce non-blank names, valid emails, and minimum ages | Jakarta `Validator` & Unit verification |
| `GlobalExceptionHandlerTest` | Exceptions map to RFC 9457 `ProblemDetail` with no leaked stack traces | `GlobalExceptionHandler` assertions |
| `ProductControllerHttpStatusTest` | Semantic HTTP status codes (200, 201 with Location, 204, 404) returned accurately | `ResponseEntity` inspection |
| `PricingAndCheckoutServiceTest` | Separation of concerns: pricing, payments, and checkout isolated from HTTP | Pure unit tests with Mockito |
| `AsyncReportControllerTest` | Async report generation completes non-blocking via `CompletableFuture` | `CompletableFuture` assertions |
| `WebCorsConfigTest` | Explicit origin whitelisting and credential security configuration | `CorsConfiguration` inspection |

## Key Test Snippets

### Proving Bean Validation on Request Payloads

```java
--8<-- "modules/06-spring-mvc/src/test/java/lab/springmvc/inputvalidation/UserControllerValidationTest.java"
```

### Proving RFC 9457 ProblemDetail Sanitization

```java
--8<-- "modules/06-spring-mvc/src/test/java/lab/springmvc/exceptionhandling/GlobalExceptionHandlerTest.java"
```

### Proving Semantic HTTP Status Codes and Location Headers

```java
--8<-- "modules/06-spring-mvc/src/test/java/lab/springmvc/httpstatus/ProductControllerHttpStatusTest.java"
```

### Proving Asynchronous Non-Blocking Processing

```java
--8<-- "modules/06-spring-mvc/src/test/java/lab/springmvc/asyncprocessing/AsyncReportControllerTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production](production.md)
