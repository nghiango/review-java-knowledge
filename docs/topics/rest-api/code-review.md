# REST API Code Review

Review each clean source before expanding its answer.

## GET mutating state

An order management controller provides an endpoint that cancels orders via `GET /api/orders/{id}/cancel`.

```java
--8<-- "modules/10-rest-api/broken-examples/get-mutating-state/OrderCancellationController.java"
```

Consider HTTP protocol method safety, web crawler pre-fetching, proxy caching, and CSRF attack surfaces.

??? warning "Reveal issues"
    **Architecture issue — GET request mutates persistent server state:** `GET` is defined as a safe, read-only method in RFC 9110 §9.3.1. Mutating state in a GET endpoint causes search engine crawlers, browser link pre-fetchers, and proxy caches to inadvertently cancel customer orders.

    **Security issue — CSRF vulnerability:** `GET` endpoints are inherently vulnerable to Cross-Site Request Forgery (CSRF) because web browsers automatically execute GET requests triggered by embedded `<img>` tags or hyperlinks on third-party sites.

    **API Contract issue — Wrong HTTP method:** State mutation should be exposed via `POST /api/orders/{id}/cancel` or `DELETE /api/orders/{id}`.

[Correct implementation](solutions.md#safe-order-controller)

---

## Status 200 everywhere anti-pattern

A product management controller returns HTTP 200 OK for every scenario (creation, deletion, missing entities, validation errors) and wraps outcomes in a custom status payload.

```java
--8<-- "modules/10-rest-api/broken-examples/status-200-everywhere/ProductManagementController.java"
```

Consider HTTP status code standards, client error handling, caching proxies, and missing `Location` headers.

??? warning "Reveal issues"
    **API Contract issue — 200 OK returned on client error & missing entity:** Returning 200 OK when a resource is not found (`ApiResponse.error("Product not found")`) prevents API gateways, load balancers, and HTTP client libraries from triggering error interceptors and retry policies.

    **API Contract issue — 200 OK returned on resource creation without Location header:** Creating a resource should return `201 Created` along with a `Location: /api/products/{id}` header pointing to the canonical URI of the new entity.

    **API Contract issue — 200 OK with custom envelope on deletion:** Successful deletion without body should return `204 No Content`.

    **Security issue — Missing input validation on request:** `CreateProductRequest` lacks `@Valid` and Bean Validation constraints, allowing blank SKUs and negative prices.

[Correct implementation](solutions.md#safe-product-controller)

---

## Missing idempotency key in mutating operations

A payment checkout controller processes credit card charges on `POST /api/payments/charges` without idempotency key validation.

```java
--8<-- "modules/10-rest-api/broken-examples/missing-idempotency-key/PaymentCheckoutController.java"
```

```java
--8<-- "modules/10-rest-api/broken-examples/missing-idempotency-key/PaymentService.java"
```

Consider network timeouts, client retry loops, distributed locking, and duplicate financial debits.

??? warning "Reveal issues"
    **Financial / Integrity issue — Non-idempotent charge endpoint causes duplicate debits:** If a network timeout occurs between client and server, client retries execute new duplicate charges against the customer account.

    **Concurrency issue — Missing atomic in-progress lock:** Concurrent identical requests execute simultaneously rather than locking on an idempotency token.

    **API Contract issue — Missing Idempotency-Key header:** Mutating financial operations must require an `Idempotency-Key` header and replay cached responses on duplicate requests.

[Correct implementation](solutions.md#idempotent-payment-controller)

---

## Entity leakage and mass assignment

A user administration controller binds JPA entities directly to `@RequestBody` and returns raw database models in `@ResponseBody`.

```java
--8<-- "modules/10-rest-api/broken-examples/entity-leakage-and-mass-assignment/UserAccount.java"
```

```java
--8<-- "modules/10-rest-api/broken-examples/entity-leakage-and-mass-assignment/UserAdminController.java"
```

Consider Mass Assignment (CWE-915), credential hash leakage, and database schema exposure.

??? warning "Reveal issues"
    **Security issue — Mass Assignment (CWE-915):** Binding JSON payloads directly to `UserAccount` entity allows untrusted clients to inject `"role": "ROLE_ADMIN"` or `"isVerified": true`, escalating administrative privileges.

    **Security issue — Sensitive data exposure:** Returning the internal domain entity directly in responses serializes `passwordHash` and internal flags to HTTP clients.

    **Architecture issue — JPA entity coupled to API contract:** Changes to database schema directly break external API clients, and Jackson may trigger `LazyInitializationException` outside active transactions.

[Correct implementation](solutions.md#safe-user-admin-controller)

---

## Inconsistent error contract

A customer service controller uses ad-hoc string and Map error responses alongside inconsistent status codes.

```java
--8<-- "modules/10-rest-api/broken-examples/inconsistent-error-contract/CustomerController.java"
```

```java
--8<-- "modules/10-rest-api/broken-examples/inconsistent-error-contract/LegacyErrorHandler.java"
```

Consider machine-readable error standards, RFC 9457 Problem Details, and type safety.

??? warning "Reveal issues"
    **API Contract issue — Inconsistent error response formats:** The API mixes plain string error bodies (`"Customer not found: " + id`), unstructured JSON Maps (`{"error": "...", "code": 4001}`), and raw Spring Boot white-label error pages.

    **Architecture issue — Lack of standard RFC 9457 Problem Details:** Error payloads omit standard attributes (`type`, `title`, `status`, `detail`, `instance`), preventing automated client error handling.

    **Maintainability issue — Ad-hoc try-catch error handling in controller:** Domain business exceptions are caught and formatted inside controller methods instead of using centralized `@RestControllerAdvice`.

[Correct implementation](solutions.md#safe-customer-controller)

---

## Missing pagination and unbounded list

A catalog controller returns full database collections without pagination limits or response envelopes.

```java
--8<-- "modules/10-rest-api/broken-examples/missing-pagination-unbounded-list/CatalogItem.java"
```

```java
--8<-- "modules/10-rest-api/broken-examples/missing-pagination-unbounded-list/CatalogController.java"
```

Consider JVM memory exhaustion (`OutOfMemoryError`), uncapped limit parameters, and deep offset degradation.

??? warning "Reveal issues"
    **Performance issue — Unbounded getAllItems endpoint:** Querying and returning the entire database collection in memory leads to Heap exhaustion (`OutOfMemoryError`), database connection starvation, and extreme JSON serialization latency.

    **API Contract issue — Missing pagination envelope:** Returning a bare array `List<T>` provides no pagination metadata (e.g., `page`, `size`, `totalElements`, `hasNext`, cursor tokens), preventing consumers from implementing reliable pagination.

    **Security issue — Uncapped limit parameter:** Allowing unconstrained limit sizes (or default 1000) exposes the API to memory and CPU resource exhaustion attacks (CWE-400 / Denial of Service).

    **Performance issue — Deep offset performance degradation:** In-memory or SQL offset pagination requires scanning and discarding `offset` rows before returning `limit` rows ($O(\text{offset} + \text{limit})$).

[Correct implementation](solutions.md#safe-catalog-controller)

---

## Optimistic concurrency and missing ETag

A collaborative document editing controller allows blind updates without evaluating version concurrency headers.

```java
--8<-- "modules/10-rest-api/broken-examples/optimistic-concurrency-missing-etag/ArticleDocument.java"
```

```java
--8<-- "modules/10-rest-api/broken-examples/optimistic-concurrency-missing-etag/DocumentEditorController.java"
```

Consider Lost Update race conditions, missing `ETag` headers on GET, and missing `If-Match` validation on PUT.

??? warning "Reveal issues"
    **Concurrency issue — Lost Update anomaly on concurrent PUT:** When two clients fetch version 1 simultaneously and submit updates sequentially, the second write silently overwrites the first write without conflict detection.

    **Performance issue — Missing ETag on GET responses:** Omission of the `ETag` HTTP header prevents downstream caches, proxies, and web clients from performing conditional requests (`If-None-Match`), wasting server bandwidth and compute.

    **API Contract issue — Missing If-Match conditional validation:** Mutating endpoints (`PUT`, `PATCH`, `DELETE`) on versioned resources should mandate the `If-Match` header to guarantee optimistic concurrency control at the HTTP protocol layer.

    **Error Handling issue — Missing HTTP 412 Precondition Failed / 428 Precondition Required:** Omitting precondition enforcement causes APIs to return improper status codes instead of standard RFC 9110 / RFC 6585 error codes.

[Correct implementation](solutions.md#safe-document-controller)
