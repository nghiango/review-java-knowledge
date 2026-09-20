# REST API Design & Architecture

## Why this matters

RESTful APIs serve as the primary communication bridge between backend services, frontend web/mobile clients, and third-party integrations. Designing production-grade REST APIs requires deep understanding of HTTP protocol semantics (safe vs idempotent methods, precise status code selection, RFC 9457 Problem Details), cache control (`Cache-Control`, `ETag`, `If-None-Match`), concurrency safety (`If-Match`, 412 Precondition Failed), resource pagination and bounding, and strict DTO boundaries to prevent mass assignment and data leakage.

Subtle design errors lead directly to severe production incidents: duplicate financial charges due to non-idempotent retry storms, JVM `OutOfMemoryError` crashes from unbounded database queries, lost updates in collaborative systems, and security breaches from exposing internal entity models.

## Core Concepts

- [HTTP Semantics, Methods, Status Codes, Caching, RFC 9457 Problem Details, Idempotency, Pagination, and Optimistic Concurrency](concepts.md)
- [Spring MVC REST Pipeline, HttpMessageConverter, Content Negotiation, HandlerMethodArgumentResolver, and ProblemDetail Response Pipeline](internals.md)

## How it works internally

Understand how Spring Boot processes REST requests through `DispatcherServlet`, resolves argument types, negotiates content via `HttpMessageConverter`, serializes RFC 9457 `ProblemDetail` via `ResponseEntityExceptionHandler`, and validates conditional preconditions in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions across 4 tiers: 6 Basic, 7 Intermediate, 6 Senior, and 4 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

Duplicate financial transactions from network retry storms, OutOfMemoryError crashes from unbounded catalog queries, lost updates from blind concurrent PUT operations, and security vulnerabilities from mass assignment are analyzed in [Production](production.md).

## Broken Examples

1. [GET mutating state](code-review.md#get-mutating-state)
2. [Status 200 everywhere anti-pattern](code-review.md#status-200-everywhere-anti-pattern)
3. [Missing idempotency key in mutating operations](code-review.md#missing-idempotency-key-in-mutating-operations)
4. [Entity leakage and mass assignment](code-review.md#entity-leakage-and-mass-assignment)
5. [Inconsistent error contract](code-review.md#inconsistent-error-contract)
6. [Missing pagination and unbounded list](code-review.md#missing-pagination-and-unbounded-list)
7. [Optimistic concurrency and missing ETag](code-review.md#optimistic-concurrency-and-missing-etag)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

Choosing between URI path vs header versioning balances discoverability against URL cleanliness; Keyset pagination provides $O(1)$ query speed at the cost of losing arbitrary page jumps; RFC 9457 standardization improves machine readability while requiring legacy client migration.

## Production Checklist

- Enforce HTTP safe/idempotent semantics: `GET`, `HEAD`, `OPTIONS` must never mutate server state.
- Return explicit semantic HTTP status codes: `201 Created` with `Location`, `204 No Content` for empty responses, `404 Not Found`, and `422 Unprocessable Entity`.
- Standardize all error responses on RFC 9457 `ProblemDetail` with `type`, `title`, `status`, `detail`, `instance`, and custom properties.
- Require `Idempotency-Key` headers on non-idempotent mutating POST endpoints (e.g. payments, orders).
- Always decouple public API contracts from internal JPA entities using dedicated request and response records.
- Cap pagination page sizes strictly (e.g. `maxSize = 100`) and return envelope metadata with next/previous links.
- Implement conditional requests via `ETag` and enforce `If-Match` headers for mutating shared resources.

## Senior-Level Questions

Explore advanced topics like distributed rate limiting algorithms, 207 Multi-Status batch processing, HTTP security headers, and asynchronous long-running job coordination in [Senior Questions](questions.md#senior).

## Exercises

Hands-on REST katas covering custom idempotency filters, RFC 9457 validation interceptors, and keyset cursor pagination in [Exercises](exercises.md).

## Related

- [Spring MVC](../spring-mvc/index.md)
- [Spring Core](../spring-core/index.md)
- [Spring Boot](../spring-boot/index.md)
