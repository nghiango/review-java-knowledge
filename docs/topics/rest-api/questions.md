# REST API Interview Questions

Four levels of interview questions covering HTTP protocol semantics, RESTful status codes, RFC 9457 Problem Details, idempotency, pagination, optimistic concurrency, and production incident remediation.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between safe and idempotent HTTP methods?

??? question "Reveal answer"
    - **Safe Methods**: Read-only operations that do not modify server state (`GET`, `HEAD`, `OPTIONS`). Clients can issue them repeatedly without side effects.
    - **Idempotent Methods**: Operations where $N > 0$ identical requests produce the exact same side-effect on server state as a single request (`GET`, `PUT`, `DELETE`, `HEAD`, `OPTIONS`).
    - `POST` is neither safe nor idempotent by default. `PATCH` is non-idempotent by default because operations like `{"increment": 1}` compound on repeat calls.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q01HttpMethodsSemantics.java"
        ```

### 2. When should a REST API return HTTP status codes `200`, `201`, `204`, `400`, `404`, and `422`?

??? question "Reveal answer"
    - `200 OK`: Standard successful response containing a body representation.
    - `201 Created`: Resource successfully created, accompanied by a `Location` header pointing to the new URI.
    - `204 No Content`: Request succeeded but response intentionally contains no body (common for `DELETE` or `PUT` when no representation is returned).
    - `400 Bad Request`: Malformed HTTP syntax, invalid JSON schema, or missing mandatory headers.
    - `404 Not Found`: Target resource URI does not exist on the server.
    - `422 Unprocessable Entity`: Syntactically valid JSON payload that violates semantic business validation rules.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q02HttpStatusCodesOverview.java"
        ```

### 3. What is the fundamental difference between `PUT` and `PATCH` in REST APIs?

??? question "Reveal answer"
    - `PUT` performs a **complete replacement** of the target resource. The client must transmit the full state representation. Any existing fields omitted from the payload are cleared or set to defaults.
    - `PATCH` applies a **partial delta modification**. The client sends only the fields that need updating, leaving omitted fields unchanged.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q03PutVsPatch.java"
        ```

### 4. What is the difference between REST and RPC style APIs?

??? question "Reveal answer"
    - **REST (Representational State Transfer)**: Resource-oriented. Models domain objects as nouns (`/api/orders`, `/api/users/42`) and uses standardized HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`) for operations.
    - **RPC (Remote Procedure Call)**: Action-oriented. Embeds operation verbs directly in the URI path (`/api/createOrder`, `/api/cancelOrder`, `/api/getUserDetails`) often mapping everything through `POST` or `GET`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q04RestVsRpc.java"
        ```

### 5. When should Path Variables be used versus Query Parameters?

??? question "Reveal answer"
    - **Path Variables (`@PathVariable`)**: Used to identify specific resources or establish hierarchical identity (`/api/departments/{deptId}/employees/{empId}`). They are integral to the resource identity.
    - **Query Parameters (`@RequestParam`)**: Used for optional filters, sorting, search tokens, projections, or pagination (`/api/employees?department=eng&sort=name,asc&page=0&size=20`).

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q05PathVariablesVsQueryParams.java"
        ```

### 6. How do HTTP caching headers (`Cache-Control`, `ETag`, `Last-Modified`) work in RESTful services?

??? question "Reveal answer"
    - `Cache-Control`: Directs clients and intermediary CDNs/proxies on caching policies (`max-age`, `no-cache`, `no-store`, `must-revalidate`, `public`, `private`).
    - `ETag`: Server-generated entity fingerprint. Clients include `If-None-Match: <etag>` in subsequent requests; if unchanged, server returns `304 Not Modified` with no body.
    - `Last-Modified`: Server timestamp of last edit. Clients send `If-Modified-Since` for conditional retrieval.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q06HttpCachingHeaders.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 7. What is RFC 9457 Problem Details and how does Spring Boot 3+ support it?

??? question "Reveal answer"
    RFC 9457 standardizes HTTP error payloads using `application/problem+json`. Standard members include `type`, `title`, `status`, `detail`, and `instance`, along with custom extension properties.
    
    Spring Boot 3+ provides native support via `org.springframework.http.ProblemDetail` and `ResponseEntityExceptionHandler`, activated globally via `spring.mvc.problemdetails.enabled=true` or custom `@RestControllerAdvice`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q07ProblemDetailsRfc9457.java"
        ```

### 8. How do you implement robust idempotency for mutating `POST` endpoints?

??? question "Reveal answer"
    1. Clients supply a unique `Idempotency-Key` UUID header.
    2. Server attempts an atomic distributed lock (e.g. Redis `SET key in_progress NX EX 60`).
    3. If locked by another worker, return `409 Conflict` or poll until completed.
    4. If already processed, retrieve and replay the cached HTTP response and status code.
    5. If fresh, process the business operation, save the resulting response to the idempotency store, and return `201 Created` with header `Idempotency-Replayed: false`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q08IdempotentPostImplementation.java"
        ```

### 9. How do `ETag` and `If-Match` headers prevent the "Lost Update" problem?

??? question "Reveal answer"
    When clients read a shared resource, the server emits an `ETag: "<version>"`. When submitting updates via `PUT` or `PATCH`, the client must send `If-Match: "<version>"`.
    
    If another concurrent transaction has modified the entity and bumped its version, the server detects the mismatch and rejects the update with `412 Precondition Failed` (or `428 Precondition Required` if omitted), preventing silent data overwrite.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q09OptimisticConcurrencyEtag.java"
        ```

### 10. What are the trade-offs between Offset-based and Keyset (Cursor) pagination?

??? question "Reveal answer"
    - **Offset-based (`OFFSET N LIMIT M`)**: Simple to implement and supports jumping to page $N$. However, DB must scan and discard $N$ rows ($O(N + M)$), resulting in severe latency on deep pages and pagination drift if records are inserted during browsing.
    - **Keyset / Cursor (`WHERE id > cursor LIMIT M`)**: Constant $O(M)$ performance via indexed B-Tree seeks and stable against concurrent writes, but cannot jump to arbitrary pages.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q10PaginationStrategies.java"
        ```

### 11. What are the common API versioning strategies and their trade-offs?

??? question "Reveal answer"
    1. **URI Path (`/api/v1/orders`)**: Most transparent, easily cached by CDNs and browser bookmarks, but creates URL proliferation.
    2. **Query Parameter (`/api/orders?version=1`)**: Simple for ad-hoc requests, but easy for clients to omit.
    3. **Custom Header (`X-API-Version: 1`)**: Clean URLs, but harder to test via plain browser URL bars and requires custom CDN cache-key configuration.
    4. **Content Negotiation (`Accept: application/vnd.company.v1+json`)**: Strictly REST-compliant, but complex for web clients and documentation tools.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q11ApiVersioningStrategies.java"
        ```

### 12. How does request body validation work in Spring MVC and how are errors formatted?

??? question "Reveal answer"
    Annotating `@RequestBody` with `@Valid` triggers Jakarta Bean Validation. If invalid, Spring raises `MethodArgumentNotValidException`.
    
    A `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` intercepts this exception, extracts all `FieldError` messages, and formats an RFC 9457 `ProblemDetail` with an `invalidParams` map.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q12RequestBodyValidation.java"
        ```

### 13. What is HATEOAS (Hypermedia As The Engine Of Application State)?

??? question "Reveal answer"
    HATEOAS represents Richardson Maturity Model Level 3. API responses embed hypermedia links (`_links`) alongside data, allowing clients to dynamically discover possible navigation transitions (e.g. `self`, `cancel`, `payment`, `receipt`) based on the current state of the resource.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q13HateoasHypermedia.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 14. How should REST APIs handle bulk and batch operations with partial failures?

??? question "Reveal answer"
    - **All-or-Nothing Transactional**: Single transaction rollback on any failure, returning `400 Bad Request` or `422 Unprocessable Entity`.
    - **Partial Success Batch (RFC 4918)**: Returns `207 Multi-Status` containing an array of individual item results, each specifying its own HTTP status code (e.g. `200 OK` for success, `422` for validation errors).

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q14BulkAndBatchOperations.java"
        ```

### 15. What algorithms are used for API Rate Limiting and how does the server communicate limit exhaustion?

??? question "Reveal answer"
    - **Algorithms**: Token Bucket, Leaky Bucket, Fixed Window Counter, Sliding Window Log/Counter.
    - When limits are breached, server responds with `429 Too Many Requests` along with standard RFC 6585 rate limiting headers: `Retry-After: <seconds>`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q15RateLimitingAlgorithms.java"
        ```

### 16. What is Mass Assignment vulnerability (CWE-915) and how do DTOs prevent it?

??? question "Reveal answer"
    Mass Assignment occurs when client HTTP request bodies are deserialized directly into persistent domain entities. Attackers add unexposed entity fields (e.g. `role: "ADMIN"`, `verified: true`, `balance: 99999`) to escalate privileges.
    
    Using explicit Java record DTOs strictly limits binding to permitted fields.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q16SecurityDtoMassAssignment.java"
        ```

### 17. What standard HTTP security headers should every production REST API enforce?

??? question "Reveal answer"
    - `Strict-Transport-Security` (HSTS): Enforces HTTPS connections.
    - `X-Content-Type-Options: nosniff`: Prevents MIME-type sniffing.
    - `X-Frame-Options: DENY`: Mitigates clickjacking via iframes.
    - `Content-Security-Policy`: Restricts resource sources.
    - `Cache-Control: no-store` on sensitive endpoints: Prevents disk caching of PII/credentials.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q17RestApiSecurityHeaders.java"
        ```

### 18. How do you design asynchronous long-running operations in REST?

??? question "Reveal answer"
    1. Client triggers operation via `POST /api/reports`.
    2. Server initiates background task and immediately responds with `202 Accepted`, `Location: /api/reports/jobs/{jobId}`, and optional `Retry-After: 30`.
    3. Client polls `GET /api/reports/jobs/{jobId}` which returns `200 OK` with progress metadata.
    4. Once finished, the job endpoint provides a `303 See Other` or download URL `Location: /api/reports/{reportId}`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q18AsyncLongRunningRestOperations.java"
        ```

### 19. How does Spring MVC internally execute HTTP Content Negotiation?

??? question "Reveal answer"
    Spring MVC uses `ContentNegotiationManager` to determine the requested media types from `Accept` headers. It then iterates registered `HttpMessageConverter` beans and checks `canWrite(Class, MediaType)`. The first converter capable of writing the target object in the negotiated media type serializes the response.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q19ContentNegotiationInternals.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenario

### 20. Production Incident: Network timeout retry storm causes duplicate payment charges. How do you diagnose and fix it?

??? question "Reveal answer"
    - **Root Cause**: Mobile/gateway clients encountered transient network timeouts on `POST /api/payments/charges` and automatically retried. Because the endpoint lacked idempotency controls, each retry created a new payment transaction.
    - **Remediation**: Require mandatory `Idempotency-Key` headers on all payment requests, acquire distributed locks via Redis, and return cached responses on replay.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q20PaymentDuplicateRetryIncident.java"
        ```

### 21. Production Incident: Catalog search endpoint triggers JVM OutOfMemoryError and database CPU saturation. How do you resolve it?

??? question "Reveal answer"
    - **Root Cause**: `GET /api/catalog` allowed unbounded queries (`findAll()` without limit) or deep offset pagination (`offset=500000`), loading hundreds of thousands of rows into JVM heap memory.
    - **Remediation**: Enforce a strict max page size cap (e.g. `@Max(100)`), return paginated metadata envelopes, and migrate deep scans to keyset cursor pagination.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q21UnboundedListOomIncident.java"
        ```

### 22. Production Incident: Concurrent users overwrite each other's changes in collaborative document editing. How do you remediate?

??? question "Reveal answer"
    - **Root Cause**: The API allowed blind `PUT` operations without version validation, creating the classic "Lost Update" race condition where the last writer silently overwrites earlier changes.
    - **Remediation**: Emit `ETag` on `GET /api/documents/{id}` and mandate `If-Match` on `PUT`. If the ETag is stale, reject with `412 Precondition Failed`.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q22LostUpdateCollaborativeEditingIncident.java"
        ```

### 23. Production Incident: Flash sale traffic spike brings down checkout API. How do you design distributed rate limiting?

??? question "Reveal answer"
    - **Root Cause**: Flash sale traffic overwhelmed payment gateways and database connection pools.
    - **Remediation**: Implement a Redis sliding window rate limiter at the API gateway or Spring MVC interceptor level, returning `429 Too Many Requests` with `Retry-After` headers for clients exceeding allowed RPS.

    ??? example "Example"
        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q23DistributedRateLimitingOutage.java"
        ```
<!-- --8<-- [end:scenarios] -->
