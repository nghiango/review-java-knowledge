# REST API Solutions & Production Implementations

This page provides production-grade solutions for the architectural and API design issues identified in [Code Review](code-review.md).

---

## Safe order controller

Adheres strictly to HTTP protocol safety and idempotency rules: `GET` operations are purely read-only and free of side-effects. Order cancellation is modeled as a state mutation via `POST /api/orders/{id}/cancel` (or `DELETE`).

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/safemethods/SafeOrderController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/safemethods/OrderService.java"
```

### Key Highlights
- `GET /api/orders/{id}` never modifies database state, protecting against browser pre-fetching and crawler cancellation storms.
- `POST /api/orders/{id}/cancel` explicitly scopes state mutation to an unsafe HTTP verb.
- `createOrder` returns `201 Created` with a standard `Location` header.

---

## Safe product controller

Adheres to standard HTTP status codes: `201 Created` on resource creation, `204 No Content` on successful deletion, `200 OK` on entity retrieval, `404 Not Found` when missing, and `400 Bad Request` with Bean Validation.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/statuscodes/SafeProductController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/statuscodes/CreateProductRequest.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/statuscodes/ProductService.java"
```

### Key Highlights
- Eliminates the "Status 200 Everywhere" anti-pattern; downstream gateways and HTTP client interceptors operate naturally on HTTP status codes.
- `DELETE` returns `204 No Content` when successful, avoiding redundant JSON payload transmission.

---

## Idempotent payment controller

Implements robust distributed idempotency controls via the `Idempotency-Key` HTTP header. Replays cached responses on retry requests and prevents duplicate financial charges.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/idempotency/IdempotentPaymentController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/idempotency/IdempotentPaymentService.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/idempotency/InMemoryIdempotencyStorage.java"
```

### Key Highlights
- Uses an atomic lock to detect concurrent in-flight requests with the same key, returning `409 Conflict`.
- Successfully executed transactions store response records and return headers `Idempotency-Key` and `Idempotency-Replayed: true` upon re-submission.

---

## Safe user admin controller

Decouples internal persistence entities from the public REST API contract using dedicated request and response records, eliminating Mass Assignment vulnerabilities (CWE-915) and entity leakage.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/dtosecurity/SafeUserAdminController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/dtosecurity/UserResponse.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/dtosecurity/UserService.java"
```

### Key Highlights
- `UserRegistrationRequest` and `UserUpdateRequest` strictly define allowable input fields; clients cannot overwrite `role` or `verified` flags.
- `UserResponse` projects only public fields, preventing `passwordHash` or internal audit timestamps from leaking to clients.

---

## Safe customer controller

Standardizes all error handling on RFC 9457 `ProblemDetail` via a centralized `@RestControllerAdvice` extending Spring's `ResponseEntityExceptionHandler`.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/problemdetails/SafeCustomerController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/problemdetails/GlobalRestExceptionHandler.java"
```

### Key Highlights
- Replaces ad-hoc string and Map error responses with standard `application/problem+json` format.
- Contextual domain information (`customerId`, `currentBalance`, `requiredAmount`, `invalidParams`) is cleanly exposed in structured problem detail extensions.

---

## Safe catalog controller

Enforces strict page size bounds (`maxSize = 100`) and returns a structured `PagedResponse<T>` envelope containing total element counts, total pages, and navigation booleans.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/pagination/SafeCatalogController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/pagination/PagedResponse.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/pagination/CatalogItemRepository.java"
```

### Key Highlights
- Hard upper bound on `size` prevents Denial of Service / OutOfMemory attacks.
- Envelope metadata allows web and mobile clients to render pagination bars and disable next/prev buttons accurately.

---

## Safe document controller

Implements optimistic concurrency control and HTTP caching using `ETag`, `If-None-Match`, and `If-Match` headers.

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/concurrency/SafeDocumentController.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/concurrency/DocumentService.java"
```

```java
--8<-- "modules/10-rest-api/src/main/java/lab/restapi/concurrency/Document.java"
```

### Key Highlights
- `GET /api/documents/{id}` emits strong `ETag` headers; matching `If-None-Match` returns lightweight `304 Not Modified`.
- `PUT /api/documents/{id}` mandates `If-Match`. If missing, returns `428 Precondition Required`; if mismatched, returns `412 Precondition Failed`.
