# Spring MVC

## Why this matters

Spring MVC is the foundational web framework for building synchronous REST APIs and enterprise web applications in the Spring ecosystem. Mastering its architecture—from the front-controller `DispatcherServlet` request lifecycle, `HandlerMapping` and `HandlerAdapter` dispatching, argument resolvers, and content negotiation to RFC 9457 `ProblemDetail` exception handling, async non-blocking offloading, and CORS security—is essential for designing reliable, performant, and secure microservices.

## Core Concepts

- [DispatcherServlet, HandlerMapping, HandlerAdapter, Interceptors, Argument Resolvers, Validation, RFC 9457 Problem Details, Async Processing, and CORS](concepts.md)
- [DispatcherServlet internal dispatch loop, HandlerExecutionChain, RequestMappingHandlerAdapter, and HttpMessageConverter pipeline](internals.md)

## How it works internally

Follow how `DispatcherServlet` receives incoming HTTP requests, resolves handler chains via `RequestMappingHandlerMapping`, executes pre-handle interceptors, binds and validates parameters via `HandlerMethodArgumentResolverComposite`, invokes controller methods, converts responses with `HttpMessageConverter`, and maps exceptions to RFC 9457 `ProblemDetail` in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions: 8 Basic, 8 Intermediate, 5 Senior, and 2 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

Tomcat worker thread pool starvation from synchronous blocking I/O, stack trace and internal database leakage in HTTP 500 error responses, improper HTTP status code usage (e.g. 200 OK with error bodies), god controllers violating single responsibility, missing validation causing database corruption, and dangerous wildcard CORS with credentials vulnerabilities are diagnosed in [Production](production.md).

## Broken Examples

1. [Missing input validation](code-review.md#missing-input-validation)
2. [Leaking stack traces in responses](code-review.md#leaking-stack-traces-in-responses)
3. [Wrong HTTP status codes](code-review.md#wrong-http-status-codes)
4. [God controller business logic](code-review.md#god-controller-business-logic)
5. [Blocking request thread](code-review.md#blocking-request-thread)
6. [CORS wildcard credentials vulnerability](code-review.md#cors-wildcard-credentials-vulnerability)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

`@ControllerAdvice` with RFC 9457 `ProblemDetail` centralizes API error contracts but requires consistent domain exception hierarchies; async processing via `DeferredResult` or `CompletableFuture` prevents servlet thread starvation under high I/O latency while introducing thread context propagation overhead; fine-grained CORS domain whitelisting prevents cross-origin session hijacking while requiring multi-environment configuration.

## Production Checklist

- Always validate incoming request DTOs using `@Valid` and Bean Validation constraints (`@NotBlank`, `@Email`, `@Size`, `@Min`).
- Centralize exception translation using `@RestControllerAdvice` and RFC 9457 `ProblemDetail`, stripping raw stack traces and internal class names from responses.
- Return semantic HTTP status codes (`201 Created` with `Location` header, `204 No Content`, `400 Bad Request`, `404 Not Found`, `409 Conflict`).
- Keep controllers thin: orchestrate HTTP routing, validation, and DTO conversion; delegate all domain business logic, pricing, and transactions to domain services.
- Offload long-running I/O or report generation to `DeferredResult`, `CompletableFuture`, or `@Async` with dedicated bounded thread pools.
- Configure explicit CORS allowed origins (`setAllowedOrigins`) with credentials enabled; never pair `allowCredentials(true)` with wildcard origins (`*`).

## Senior-Level Questions

Explore advanced topics like `HandlerMethodArgumentResolver` customization, asynchronous non-blocking request processing lifecycle, RFC 9457 `ProblemDetail` implementation, and content negotiation mechanisms in [Senior Questions](questions.md#senior).

## Exercises

Hands-on Spring MVC katas to practice custom argument resolvers, async deferred controllers, and centralized exception handling in [Exercises](exercises.md).

## Related

- [Spring Core](../spring-core/index.md)
- [Spring Boot](../spring-boot/index.md)
- [Security Issues](../../issues/security.md)
- [Reliability Issues](../../issues/reliability.md)
- [Performance Issues](../../issues/performance.md)
- [Maintainability Issues](../../issues/maintainability.md)
