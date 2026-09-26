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

### 24. How does RFC 9457 Problem Details format structured validation errors and custom extension properties?

??? question "Reveal answer"

    **Short Answer:** RFC 9457 specifies standard machine-readable JSON error payloads (`type`, `title`, `status`, `detail`, `instance`). Applications attach custom extension members (e.g. `invalidParams` arrays with field name and reason, `correlationId`, `errorCode`) via `ProblemDetail.setProperty()`.

    **Internal Mechanism:** Jackson serializes `ProblemDetail.getProperties()` as first-class JSON keys at the root of the error object, ensuring standardized client parsing without proprietary envelope wrappers.

    **Common Mistake:** Returning ad-hoc custom error maps or plain text error strings, forcing API consumers to build proprietary exception parsers for every microservice. [Concepts](/topics/rest-api/concepts.md#3-rfc-9457-problem-details-for-http-apis)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q24ProblemDetailFieldErrorsExample.java"
        ```

### 25. What is the IETF Idempotency-Key specification and how does it prevent duplicate mutating operations?

??? question "Reveal answer"

    **Short Answer:** The client generates a unique UUID in the `Idempotency-Key` header for mutating `POST`/`PATCH` requests. The server records the key and request payload hash in a distributed cache; if the client retries after a network timeout, the server replays the original cached response without re-executing the operation.

    **Internal Mechanism:** If a duplicate key arrives with the exact same payload hash, the server returns the cached HTTP response. If the key arrives with a *different* payload hash, the server rejects it with `422 Unprocessable Entity` (Idempotency Key Conflict).

    **Common Mistake:** Relying on client retry logic without server-side idempotency keys for payment or order placement endpoints, causing duplicate financial transactions on network timeouts. [Concepts](/topics/rest-api/concepts.md#4-idempotency-in-mutating-operations)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q25IetfIdempotencyKeySpecExample.java"
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

### 26. What are the protocol differences between Strong and Weak ETags and how do they prevent Lost Updates via If-Match?

??? question "Reveal answer"

    **Short Answer:** Strong ETags (`"hash"`) require byte-for-byte exact equality across representations and support byte-range requests. Weak ETags (`W/"hash"`) guarantee semantic equivalence (e.g. gzip-compressed vs uncompressed, or cosmetic JSON whitespace changes). Both prevent Lost Updates when paired with conditional `If-Match` headers.

    **Deep Explanation:** In collaborative editing or resource updates, Client A and Client B read version 1 (`ETag: "v1"`). If Client A sends `PUT` with `If-Match: "v1"`, the server updates the resource to version 2 (`ETag: "v2"`). When Client B later attempts `PUT` with `If-Match: "v1"`, the server rejects it with `412 Precondition Failed`, forcing Client B to fetch the latest state instead of overwriting Client A's changes.

    **Internal Mechanism:** The web server compares the client's `If-Match` header value with the resource's current ETag validator before executing the controller handler method.

    **Example:** [Optimistic concurrency control with ETags](/topics/rest-api/concepts.md#5-http-caching-and-optimistic-concurrency-control).

    **Common Mistake:** Using Weak ETags for byte-range download requests (e.g. video streaming or resume downloads), which is prohibited by RFC 9110.

    **Production Consideration:** Generate Strong ETags using SHA-256 hashes of the canonical database representation or entity `@Version` numbers.

    **Follow-up Questions:**
    - How do database isolation levels and write skew relate to HTTP conditional updates? See [Database / SQL: Isolation Anomalies](/topics/database-sql/questions.md#9-what-are-the-four-database-isolation-levels-and-their-corresponding-anomalies)
    - How does Spring MVC support conditional requests with ShallowEtagHeaderFilter? See [Spring MVC: Caching Headers](/topics/spring-mvc/questions.md#16-how-do-http-caching-headers-cache-control-etag-work-in-spring-mvc)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q26StrongVsWeakEtagConditionalsExample.java"
        ```

### 27. When should asynchronous REST APIs use polling status endpoints vs webhook callback delivery?

??? question "Reveal answer"

    **Short Answer:** Polling (`202 Accepted`, `Location: /api/tasks/{id}`, `Retry-After: 30`) is pull-based, client-driven, and works across corporate firewalls without requiring client public IP ingress. Webhooks are push-based, event-driven, and eliminate polling server load, but require client public HTTPS endpoints and HMAC signature verification.

    **Deep Explanation:** For long-running batch jobs (e.g. video transcoding, report generation), returning `202 Accepted` decouples the client from long HTTP connections. If operations take seconds to minutes, polling with exponential backoff is straightforward. For hours-long operations or B2B integrations, Webhooks deliver results immediately upon completion without wasting network bandwidth on thousands of polling requests.

    **Internal Mechanism:** Webhook dispatchers sign payloads with HMAC-SHA256 (`X-Hub-Signature-256`) using a shared secret; the client recalculates the hash to verify authenticity before processing.

    **Example:** [Asynchronous REST operations](/topics/rest-api/questions.md#18-how-do-you-design-asynchronous-long-running-operations-in-rest).

    **Common Mistake:** Implementing webhooks without retry backoff or dead-letter queues, permanently dropping events when client webhooks experience transient downtime.

    **Production Consideration:** Support both: return `202 Accepted` with a polling `Location` URL, while optionally accepting a `callback_url` in the initial request body.

    **Follow-up Questions:**
    - How does Servlet 3.0+ async processing release container worker threads while long jobs run? See [Spring MVC: Servlet Async Thread Model](/topics/spring-mvc/questions.md#18-how-does-servlet-30-asynchronous-request-processing-decouple-container-worker-threads)
    - How do circuit breakers protect outgoing webhook delivery services from cascading timeouts? See [Resilience: Fault Tolerance Patterns](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q27WebhookVsPollingAsyncPatternExample.java"
        ```

### 28. How does Jackson polymorphic deserialization introduce security vulnerabilities and how do type discriminators fix them?

??? question "Reveal answer"

    **Short Answer:** Using `@JsonTypeInfo(use = Id.CLASS)` or `enableDefaultTyping()` allows JSON payloads to specify arbitrary Java class names (`@class: "org.apache.commons.collections...Gadget"`). Attackers supply malicious gadget classes executed reflectively during deserialization, leading to Remote Code Execution (RCE). Fix by using logical type names (`Id.NAME`) with strict `@JsonSubTypes` whitelists.

    **Deep Explanation:** In polymorphic models (e.g. `Notification` with `EmailNotification` and `SmsNotification`), Jackson needs to know which concrete subclass to instantiate. When configured with class-name typing, Jackson instantiates whatever class the JSON payload specifies. If vulnerable gadget libraries exist on the classpath, deserialization executes arbitrary bytecode.

    **Internal Mechanism:** `Id.NAME` uses logical string tokens (e.g. `"type": "EMAIL"`) mapped to compile-time registered classes in `@JsonSubTypes`, completely rejecting unlisted class names.

    **Example:** [DTO projection and mass assignment](/topics/rest-api/concepts.md#7-security-dto-projection-mass-assignment-prevention).

    **Common Mistake:** Enabling default typing on `ObjectMapper` globally to support generic polymorphic lists in microservices, exposing every endpoint to deserialization gadget attacks.

    **Production Consideration:** Enforce strict type validation using Java records and sealed interface hierarchies with `@JsonTypeInfo(use = Id.NAME)` and `@JsonSubTypes`.

    **Follow-up Questions:**
    - How do Java serialization filters (`jdk.serialFilter`) defend against legacy RMI gadget chains? See [Core Java: Serialization Security](/topics/core-java/questions.md#6-legacy-java-native-serialization-introduces-remote-code-execution-and-invariant-bypass-risks-how-do-you-safely-eliminate-it)
    - How does Spring Security authenticate and authorize token payloads before deserialization? See [Spring Security: Authentication](/topics/spring-security/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q28PolymorphicDeserializationSecurityExample.java"
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

### 29. Production Incident: Payment timeout retry storm causes multi-charge transactions due to missing idempotency keys

??? question "Reveal answer"

    **Short Answer:** A mobile banking client submitted `POST /api/payments/charge`; due to an upstream gateway delay of 3.2 seconds, the client timed out at 3.0 seconds and retried three times. Because the endpoint lacked idempotency controls, all four requests were executed, charging the customer's credit card four times for a single order.

    **Deep Explanation:** In distributed systems, network timeouts are ambiguous: the client does not know whether the request failed before reaching the server, while executing, or while transmitting the response back. Without idempotency enforcement, any retry on a mutating endpoint duplicates the financial transaction.

    **Internal Mechanism:** The payment processing engine executed four separate bank authorization requests because each incoming HTTP request was assigned a new transaction ID.

    **Example:** [Idempotent POST implementation](/topics/rest-api/code-review.md).

    **Common Mistake:** Trusting clients not to retry or attempting deduplication solely by order ID without distributed locking, allowing concurrent retries to execute in parallel.

    **Production Consideration:** Require an `Idempotency-Key` header on all non-safe mutating requests. Store the key in Redis with a short distributed lock (e.g. 10s) and long-lived response cache (e.g. 24 hours). Return the cached response with the original HTTP status on any replay.

    **Follow-up Questions:**
    - How does the Transactional Outbox Pattern ensure atomic payment state and event publishing? See [Spring Transactions: Transactional Outbox](/topics/spring-transactions/questions.md#26-how-does-the-transactional-outbox-pattern-guarantee-atomic-event-emission-and-what-are-the-trade-offs-of-cdc-vs-polling)
    - How do distributed Redis locks (`SET NX PX`) prevent race conditions between simultaneous retries? See [Caching / Redis: Distributed Locks](/topics/caching-redis/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q29RetryStormPaymentDuplicateScenarioExample.java"
        ```

### 30. Production Incident: Deeply nested relational API expansion causes JVM heap memory saturation and high GC pauses

??? question "Reveal answer"

    **Short Answer:** A mobile client requested `/api/catalog?expand=categories.products.variants.inventories.warehouses`. Hydrating 5 levels of unpaged entity associations instantiated over 500,000 Java objects in a single HTTP request, causing 4-second GC Stop-The-World pauses and connection resets.

    **Deep Explanation:** In REST and GraphQL APIs, allowing unbounded relation expansion (`?expand=...` or nested GraphQL selection sets) permits clients to trigger Cartesian products and massive object graph allocations. When multiple concurrent clients make nested calls, young generation allocations churn rapidly into Old generation, forcing frequent Full GC pauses.

    **Internal Mechanism:** Unbounded JPA association traversal or recursive DTO mapping instantiates hundreds of thousands of heap objects, consuming gigabytes of memory for a single response.

    **Example:** [Unbounded list OOM incident](/topics/rest-api/code-review.md).

    **Common Mistake:** Supporting nested relation expansions without depth limits, pagination on child collections, or field projection limits.

    **Production Consideration:** Limit expansion depth to a maximum of 2 levels; enforce strict pagination on all child collections (`max_items=20`); and compute payload complexity budgets at the API gateway before executing queries.

    **Follow-up Questions:**
    - Why does combining JOIN FETCH on collections with Spring Data pagination cause memory leaks? See [JPA / Hibernate: JOIN FETCH Pagination Hazard](/topics/jpa-hibernate/questions.md#27-why-is-combining-join-fetch-on-a-to-many-association-with-pagination-dangerous-and-how-do-you-redesign-it-using-two-phase-id-pagination-or-batch-fetching)
    - How do JVM garbage collectors (G1 vs ZGC) respond to massive object allocation spikes? See [JVM: Garbage Collection](/topics/jvm/questions.md#3-how-is-jvm-memory-divided-between-stack-and-heap)

    ??? example "Example"

        ```java
        --8<-- "modules/10-rest-api/src/examples/java/lab/restapi/questions/Q30NestedQueryMemorySaturationScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
