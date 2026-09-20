# REST APIs in Production

Operational incidents, failure modes, scalability pitfalls, and resilience patterns for enterprise RESTful services.

## 1. Non-Idempotent Network Retry Storms & Financial Duplication

When mobile apps, frontends, or API gateways experience transient TCP disconnects or gateway timeouts (504), HTTP client libraries automatically retry uncompleted requests.

### Incident Walkthrough
1. Client issues `POST /api/payments/charges` for $100.
2. The server finishes processing the transaction in the database and third-party payment gateway, but the network connection drops before the HTTP 201 response reaches the client.
3. The client's retry policy fires 2 additional times, generating 3 distinct database rows and charging the customer $300.

### Remediation Strategy
- **Mandate `Idempotency-Key`**: Require an `Idempotency-Key` header on all mutating POST endpoints.
- **Distributed Lock with TTL**: Acquire a lock (e.g. Redis `SET key in_progress NX EX 60`) before beginning execution.
- **Deduplication Store**: Cache the HTTP status and serialized response payload for 24 hours. Subsequent requests with the same key replay the cached response with `Idempotency-Replayed: true`.

---

## 2. Unbounded Collection Queries & JVM OutOfMemoryError Outages

Endpoints that return bare `List<T>` collections without hard pagination limits inevitably cause outages as datasets grow.

### Incident Walkthrough
1. An endpoint `GET /api/catalog` was tested in development with 50 products.
2. In production, the catalog grows to 1,500,000 items.
3. A routine query loads 1.5 million ORM entities into JVM heap memory.
4. Garbage collection thrashing triggers Stop-The-World pauses, exceeding Kubernetes liveness probe timeouts and crashing the pod with `java.lang.OutOfMemoryError: Java heap space`.

### Remediation Strategy
- **Hard Upper Bound**: Enforce `@Max(100)` on all pagination `size` parameters.
- **Keyset / Cursor Pagination**: For high-volume continuous feeds, migrate from offset pagination (`OFFSET N LIMIT M`) to keyset pagination (`WHERE id > cursor LIMIT M`).
- **Response Envelopes**: Always return a `PagedResponse<T>` containing `page`, `size`, `totalElements`, and navigation cursors.

---

## 3. Lost Updates in High-Concurrency Editing

When multiple users or services read and update the same entity concurrently via `PUT` or `PATCH`, the last write wins, silently destroying intermediate modifications.

### Incident Walkthrough
1. User A and User B fetch Document version 1 simultaneously.
2. User A updates the title and submits `PUT /api/documents/1`. The server saves version 2.
3. User B updates the body (based on version 1) and submits `PUT /api/documents/1`.
4. Without concurrency checks, User B's write overwrites version 2, erasing User A's changes without warning.

### Remediation Strategy
- **Emit `ETag` on GET**: Return `ETag: "<version>"` or `ETag: "<hash>"`.
- **Enforce `If-Match` on Mutation**: Require `If-Match` header on `PUT`/`PATCH`/`DELETE`. If omitted, return `428 Precondition Required`. If mismatched, return `412 Precondition Failed`.

---

## 4. Mass Assignment & Data Leakage Security Vulnerabilities

Binding incoming HTTP request payloads directly to JPA entities (CWE-915) allows attackers to inject administrative properties.

### Security Remediation
- **Strict Separation**: Never use `@Entity` classes as `@RequestBody` or `@ResponseBody`.
- **Java Records for DTOs**: Define explicit immutable record types for each operation (`UserRegistrationRequest`, `UserUpdateRequest`, `UserResponse`).
- **Field Whitelisting**: Map request DTOs to entities inside domain services with explicit setter calls.
