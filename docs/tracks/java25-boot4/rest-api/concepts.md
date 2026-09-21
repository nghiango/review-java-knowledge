# REST API Concepts in Spring Boot 4

!!! info "Delta from baseline"
    Baseline module [`modules/10-rest-api`](../../../topics/rest-api/concepts.md) covers core REST semantics: safe/idempotent methods, resource URIs, standard status codes, and manual header handling.
    This page covers the **Spring Boot 4.0 / Spring Framework 7 concepts**: declarative HTTP clients, RFC 8594 lifecycle signaling, native version resolution, and JSpecify-driven nullability contracts.

---

## 1. Modern Declarative HTTP Interfaces (`@HttpExchange`)

In Spring Boot 3.0, Spring Framework introduced HTTP interfaces using `@HttpExchange`, `@GetExchange`, and `@PostExchange`. However, bootstrapping them required significant manual boilerplate: building a `RestClient`, creating a `RestClientAdapter`, wrapping it in `HttpServiceProxyFactory.builderFor(adapter).build()`, and finally calling `createClient(MyInterface.class)`.

In Spring Boot 4 and Spring Framework 7:
1. **Simplified Client Generation**: Declarative interfaces can be registered and configured with unified client builders and autoconfiguration.
2. **Explicit Resilience & Timeout Bounds**: Client builders enforce mandatory connection and socket read timeouts, preventing virtual threads from remaining pinned or stalled indefinitely when downstream APIs degrade.
3. **Structured RFC 9457 Error Propagation**: Instead of throwing untyped `RestClientResponseException`, HTTP interface proxies natively capture and deserialize downstream `ProblemDetail` payloads into domain exceptions.

---

## 2. Native REST API Versioning & RFC 8594

Prior to Spring Framework 7, API versioning was implemented via ad-hoc URL paths (`/v1/orders`), custom request interceptors, or imperative `if/else` checks on headers (`@RequestHeader("X-API-Version")`).

Spring Framework 7 provides first-class API versioning:
- **Declarative Route Mappings**: Controllers declare supported versions using header matchers or vendor media types directly on `@GetMapping` / `@RequestMapping`.
- **RFC 8594 Deprecation and Sunset Headers**:
  - `Deprecation`: Communicates that an endpoint version is deprecated (formatted as `@<epochSeconds>` or `true`).
  - `Sunset`: Specifies the exact future timestamp (RFC 1123 / IMF-fixdate format) when the API endpoint will be decommissioned and return `410 Gone` or `404 Not Found`.
  - `Link`: References documentation explaining migration steps (`rel="deprecation"`).

```http
HTTP/1.1 200 OK
Content-Type: application/json
Deprecation: @1767225600
Sunset: Thu, 31 Dec 2026 23:59:59 GMT
Link: <https://api.example.com/docs/v2-migration>; rel="deprecation"
```

---

## 3. Strict DTO Contracts with JSpecify & Jackson 3

In Spring Boot 4:
- REST DTOs are declared as Java records annotated with JSpecify `@NullMarked` at the package level (`package-info.java`).
- Fields that may be absent in client requests or responses must be explicitly marked with `@Nullable`.
- Jackson 3 introduces immutability by default for `ObjectMapper` and direct record constructor invocation, removing obsolete reflection-based field injection and preventing mass assignment vulnerabilities.
