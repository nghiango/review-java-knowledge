# Spring MVC Code Review

Review each clean source before expanding its answer.

## Missing input validation

A user registration endpoint accepts unvalidated payloads, permitting blank usernames and malformed email addresses directly into domain processing.

```java
--8<-- "modules/06-spring-mvc/broken-examples/missing-input-validation/CreateUserRequest.java"
```

```java
--8<-- "modules/06-spring-mvc/broken-examples/missing-input-validation/UserController.java"
```

Consider Bean Validation annotations, `@Valid` on controller parameters, and defense-in-depth at API boundaries.

??? warning "Reveal issues"
    **Security issue — missing input validation annotations:** `CreateUserRequest` does not declare `@NotBlank`, `@Email`, or `@Size` constraints, allowing malicious or malformed data into persistence layers.

    **Security issue — missing @Valid on controller method:** `UserController.createUser` lacks `@Valid` or `@Validated`, meaning Spring MVC will not trigger request body validation.

[Correct implementation](solutions.md#validated-user-registration)

---

## Leaking stack traces in responses

An order controller catches internal exceptions and returns raw system exception messages and full stack traces directly to API clients.

```java
--8<-- "modules/06-spring-mvc/broken-examples/leaking-stack-traces/OrderController.java"
```

```java
--8<-- "modules/06-spring-mvc/broken-examples/leaking-stack-traces/GlobalExceptionHandler.java"
```

Consider information leakage vulnerabilities, exception interception via `@RestControllerAdvice`, and RFC 9457 `ProblemDetail`.

??? warning "Reveal issues"
    **Security issue — internal exception details leaked to client:** `GlobalExceptionHandler` and `OrderController` expose raw database error messages and stack traces to HTTP callers, revealing database schema, driver names, and internal package paths.

    **Maintainability issue — ad-hoc try-catch in controller:** Controller methods catch generic `Exception` instead of letting domain exceptions bubble to centralized handlers.

    **Architecture fix — RFC 9457 ProblemDetail:** Centralize exception handling using `@RestControllerAdvice` returning sanitized `ProblemDetail` instances.

[Correct implementation](solutions.md#sanitized-exception-handling)

---

## Wrong HTTP status codes

A product catalog controller returns HTTP 200 OK for missing products, resource creation, and business failures, burying actual status in a custom JSON wrapper.

```java
--8<-- "modules/06-spring-mvc/broken-examples/wrong-http-status-codes/ProductController.java"
```

```java
--8<-- "modules/06-spring-mvc/broken-examples/wrong-http-status-codes/ApiResponse.java"
```

Consider HTTP protocol semantics, caching intermediaries, REST conventions, and `Location` headers for resource creation.

??? warning "Reveal issues"
    **Architecture issue — HTTP 200 OK returned for not found resource:** `ProductController.getProduct()` returns HTTP 200 OK with an error message when a product is missing, misleading caching proxies, API gateways, and monitoring dashboards.

    **Architecture issue — HTTP 200 OK returned for resource creation:** `createProduct()` returns 200 OK instead of `201 Created` with a `Location` header.

    **Architecture issue — HTTP 200 OK returned on deletion failure:** `deleteProduct()` returns 200 OK even when deletion fails.

[Correct implementation](solutions.md#semantic-http-status-responses)

---

## God controller business logic

A checkout controller contains monolithic pricing logic, discount calculations, direct database persistence, and payment gateway calls.

```java
--8<-- "modules/06-spring-mvc/broken-examples/god-controller-business-logic/CheckoutController.java"
```

Consider Single Responsibility Principle, separation of transport and domain layers, testability, and transactional boundaries.

??? warning "Reveal issues"
    **Maintainability issue — god controller with embedded domain logic:** `CheckoutController` performs discount calculations, order persistence, inventory decrement, and credit card charging directly inside the HTTP handler.

    **Maintainability issue — tight coupling and poor testability:** Business rules cannot be tested without mocking HTTP request contexts or starting web servers.

    **Architecture fix — domain service delegation:** Extract pricing logic to `PricingService`, payment orchestration to `PaymentService`, and checkout workflow to `CheckoutService`.

[Correct implementation](solutions.md#controller-service-separation)

---

## Blocking request thread

A reporting controller performs synchronous multi-second data aggregation on the incoming Tomcat worker thread without timeouts.

```java
--8<-- "modules/06-spring-mvc/broken-examples/blocking-request-thread/ReportController.java"
```

```java
--8<-- "modules/06-spring-mvc/broken-examples/blocking-request-thread/ReportGenerationService.java"
```

Consider Tomcat thread pool capacity, thread starvation incidents, and asynchronous processing using `DeferredResult` or `CompletableFuture`.

??? warning "Reveal issues"
    **Performance issue — blocking servlet worker thread:** `ReportController.generateReport()` performs expensive multi-second computation on the container worker thread, risking worker pool exhaustion under concurrent load.

    **Reliability issue — missing execution timeouts:** No timeout mechanism is configured to cancel or reject stuck report queries.

    **Performance fix — non-blocking async execution:** Use `DeferredResult` or `CompletableFuture` with a dedicated bounded thread pool to release servlet worker threads immediately.

[Correct implementation](solutions.md#async-report-generation)

---

## CORS wildcard credentials vulnerability

A financial account controller configures wildcard origins (`*`) while enabling credentials, exposing authenticated session cookies to arbitrary external websites.

```java
--8<-- "modules/06-spring-mvc/broken-examples/cors-wildcard-credentials/WebCorsConfig.java"
```

```java
--8<-- "modules/06-spring-mvc/broken-examples/cors-wildcard-credentials/AccountController.java"
```

Consider cross-origin security models, browser credential policies, and explicit origin whitelisting.

??? warning "Reveal issues"
    **Security issue — wildcard origin with credentials allowed:** `WebCorsConfig` specifies `allowedOriginPatterns("*")` paired with `allowCredentials(true)`, which creates a cross-origin security vulnerability allowing malicious domains to access sensitive endpoints with victim credentials.

    **Security fix — explicit origin whitelist:** Restrict CORS configuration to explicit trusted domains (e.g. `https://app.example.com`) and specific HTTP methods.

[Correct implementation](solutions.md#secure-cors-configuration)
