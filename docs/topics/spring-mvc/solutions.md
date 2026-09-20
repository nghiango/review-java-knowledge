# Spring MVC Solutions

Production-grade implementations corresponding to the code review exercises.

## Validated user registration

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/inputvalidation/CreateUserRequest.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/inputvalidation/UserResponse.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/inputvalidation/UserService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/inputvalidation/UserController.java"
```

### Why it works

1. **Declarative Validation Constraints**: `CreateUserRequest` uses `@NotBlank`, `@Email`, `@Size`, and `@Min` on record fields to reject malformed inputs before reaching domain layers.
2. **`@Valid` Activation**: `UserController.createUser` marks the request parameter with `@Valid`, instructing `DispatcherServlet` and `HandlerMethodArgumentResolver` to invoke Jakarta Validation.
3. **Immutability and Semantic Status**: The controller returns `201 Created` with a `Location` header pointing to the created user resource.

### Trade-offs

Declarative annotations validate syntax and format easily, but complex multi-field cross-validation or asynchronous database checks (e.g. unique email verification) require custom validators or service-layer validation.

---

## Sanitized exception handling

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/exceptionhandling/OrderNotFoundException.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/exceptionhandling/OrderProcessingService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/exceptionhandling/GlobalExceptionHandler.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/exceptionhandling/OrderController.java"
```

### Why it works

1. **Centralized `@RestControllerAdvice`**: Removes ad-hoc `try-catch` blocks from controller methods, delegating all error handling to `GlobalExceptionHandler`.
2. **RFC 9457 ProblemDetail**: Uses `ProblemDetail.forStatusAndDetail(...)` with specific HTTP status codes (`404 Not Found`, `500 Internal Server Error`).
3. **No Information Leakage**: Server faults return a generic sanitized message (`An internal server error occurred`), logging full stack traces internally without exposing them in HTTP responses.

### Trade-offs

Centralized advice requires disciplined exception hierarchy design across all domain modules to avoid catching overly broad base exceptions.

---

## Semantic HTTP status responses

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/httpstatus/Product.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/httpstatus/ProductNotFoundException.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/httpstatus/ProductService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/httpstatus/ProductController.java"
```

### Why it works

1. **Accurate HTTP Statuses**: Missing resources throw domain exceptions mapped to `404 Not Found`; creations return `201 Created`; deletions return `204 No Content`.
2. **Resource Location Header**: Successful POST requests include a `Location` URI header directing clients to the newly created resource.
3. **Cache & Proxy Friendly**: Downstream caches, gateways, and client libraries rely on standard HTTP status codes rather than parsing custom JSON payload fields.

### Trade-offs

Returning standard HTTP status codes instead of universal `200 OK` wrappers requires frontend clients to inspect HTTP status codes, which is standard RFC-compliant REST practice.

---

## Controller service separation

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/OrderRequest.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/OrderResponse.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/PricingService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/PaymentService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/CheckoutService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/controllerseparation/CheckoutController.java"
```

### Why it works

1. **Single Responsibility**: `CheckoutController` is responsible strictly for HTTP transport, parameter validation, and status code mapping.
2. **Dedicated Domain Services**: `PricingService` calculates discounts and taxes; `PaymentService` manages external payment integrations; `CheckoutService` orchestrates the order workflow.
3. **Testability**: Pricing and checkout business rules can be verified in fast, isolated unit tests without Spring or web harnesses.

### Trade-offs

Increases the number of classes and service interfaces, but dramatically improves cohesion, maintainability, and testability.

---

## Async report generation

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/asyncprocessing/AsyncReportService.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/asyncprocessing/AsyncReportController.java"
```

### Why it works

1. **Non-Blocking Servlet Thread**: `AsyncReportController` returns `CompletableFuture<ResponseEntity<ReportResult>>`, releasing the Tomcat worker thread immediately.
2. **Dedicated Bounded Thread Pool**: Long-running computation is offloaded to a separate `ExecutorService` rather than consuming HTTP request threads.
3. **Timeouts and Fallbacks**: Asynchronous tasks can be bounded with execution timeouts (`orTimeout(5, TimeUnit.SECONDS)`), preventing runaway thread accumulation.

### Trade-offs

Asynchronous processing introduces thread context switching and requires explicit propagation of `SecurityContext` and tracing context (MDC / OpenTelemetry) across thread boundaries.

---

## Secure CORS configuration

### Implementation

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/corssecurity/WebCorsConfig.java"
```

```java
--8<-- "modules/06-spring-mvc/src/main/java/lab/springmvc/corssecurity/AccountController.java"
```

### Why it works

1. **Explicit Origin Whitelisting**: `WebCorsConfig` specifies explicit trusted domains (`https://app.example.com`) rather than wildcards.
2. **Method & Header Restriction**: Only required HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`) and standard headers (`Authorization`, `Content-Type`) are permitted.
3. **Preflight Caching**: Sets `maxAge(3600)` to reduce overhead from recurring `OPTIONS` preflight requests.

### Trade-offs

Static origin whitelisting requires updating configuration or externalizing allowed origin lists via environment properties when adding new frontend domains.
