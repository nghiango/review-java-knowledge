# REST API Interview Questions — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline questions in [`docs/topics/rest-api/questions.md`](../../../topics/rest-api/questions.md) cover HTTP methods, status codes, RFC 9457 error structures, ETag, and idempotency keys.
    These questions focus on the **Spring Boot 4.0 / Spring Framework 7 & Java 25 delta**: declarative `@HttpExchange` clients, native API versioning, RFC 8594 lifecycle headers, JSpecify null-safety, and client migration.

---

## Basic Questions

### 1. How does Spring Boot 4 / Spring Framework 7 declarative @HttpExchange simplify REST client definitions compared to RestClient?

??? question "Reveal answer"
    In Spring Boot 4 / Framework 7, `@HttpExchange` enables declarative type-safe HTTP client interfaces without writing imperative client call chains. Developers declare an annotated Java interface with `@GetExchange`, `@PostExchange`, `@PutExchange`, or `@DeleteExchange`. The framework generates the dynamic proxy, serializes request arguments into JSON payloads, query parameters, or path variables, and maps HTTP response payloads directly into immutable records.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q01HttpExchangeBasicsExample.java"

### 2. What are the primary REST API versioning strategies supported natively in Spring Framework 7, and how do their trade-offs compare?

??? question "Reveal answer"
    Spring Framework 7 natively supports four core API versioning strategies:
    1. **URI Path Segment** (e.g. `/api/v1/orders`): Highly visible, intuitive, and works flawlessly with all HTTP caches, but pollutes URI resource identity.
    2. **Request Header** (e.g. `X-API-Version: 2`): Keeps URIs clean and canonical, but requires `Vary: X-API-Version` response headers to prevent CDN caching collisions.
    3. **Query Parameter** (e.g. `/api/orders?version=2`): Easy for ad-hoc browser testing, but conflates query filtering with schema versioning.
    4. **Media Type Content Negotiation** (e.g. `Accept: application/vnd.company.v2+json`): Strictly aligns with REST HATEOAS, but increases client header configuration complexity.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q02NativeApiVersioningStrategiesExample.java"

### 3. How do RFC 8594 Sunset and Deprecation headers communicate REST endpoint lifecycle states to API consumers?

??? question "Reveal answer"
    RFC 8594 defines standard HTTP response headers for communicating API deprecation and retirement:
    - **`Deprecation`**: Informs clients that the endpoint is deprecated. Can be a boolean or a Unix epoch timestamp (e.g. `@1767225600`).
    - **`Sunset`**: Defines the hard decommissioning date formatted as an HTTP IMF-fixdate (RFC 7231). After this date, the endpoint may return `410 Gone` or `404 Not Found`.
    - **`Link`**: Specifies a URL pointing to migration documentation with `rel="deprecation"`.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q03Rfc8594DeprecationSunsetHeadersExample.java"

### 4. How do JSpecify nullability annotations (@NullMarked and @Nullable) strengthen REST DTO contracts in Spring Boot 4?

??? question "Reveal answer"
    By placing `@NullMarked` on `package-info.java`, every record component and method parameter in REST DTOs is non-null by default. Optional fields must be explicitly marked with `@Nullable`. This allows static analysis tools (like NullAway and Error Prone) and modern IDEs to catch missing attributes and null-pointer bugs at compile time, eliminating runtime NPEs in JSON deserialization and response rendering.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q04JSpecifyRestContractsExample.java"

---

## Intermediate Questions

### 5. How should declarative HTTP interface clients capture and propagate remote RFC 9457 Problem Details without swallowing error diagnostics?

??? question "Reveal answer"
    Declarative HTTP interface clients should not rely on generic `catch (RuntimeException ex)` blocks that discard the underlying HTTP status and error body. Instead, configure a `defaultStatusHandler` or custom `ResponseErrorHandler` on the underlying `RestClient`. When downstream calls return HTTP 4xx or 5xx, the handler parses the remote RFC 9457 `ProblemDetail` (title, detail, type, custom extension properties) and re-throws a typed domain exception carrying the structured problem detail.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q05DeclarativeClientErrorHandlingExample.java"

### 6. What are the concurrency considerations and timeout configurations when invoking declarative HTTP interface clients on virtual threads?

??? question "Reveal answer"
    Virtual threads are lightweight, but they still consume socket connections and OS file descriptors during blocking HTTP calls. If declarative HTTP clients are instantiated without explicit connect and socket read timeouts, network partitions or hanging servers will cause thousands of virtual threads to pile up in memory, exhausting thread limits and upstream connection pools. Clients must configure explicit bounded timeouts via `ClientHttpRequestFactory`.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q06HttpExchangeVirtualThreadSafetyExample.java"

### 7. How does Jackson 3 modernize JSON serialization for records and immutable collections in Spring Boot 4 REST APIs?

??? question "Reveal answer"
    Jackson 3 (`tools.jackson.*`) adopts an immutable-by-default architecture where `ObjectMapper` instances are configured via immutable builders (`JsonMapper.builder()`). For Java records and immutable collections (`List.copyOf()`, `Set.of()`), Jackson 3 uses canonical record constructors directly without reflection workarounds, ensuring thread safety and preventing unintended state mutations during payload unmarshalling.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q07Jackson3RestPayloadEvolutionExample.java"

### 8. How does vendor media-type content negotiation versioning work in REST, and how is it implemented cleanly in Spring Boot 4?

??? question "Reveal answer"
    Vendor media-type versioning uses custom MIME types in the `Accept` and `Content-Type` headers (e.g. `application/vnd.company.order.v2+json`). Spring Boot 4 controllers map these cleanly using the `produces` and `consumes` attributes on `@GetMapping` / `@PostMapping`. This enables distinct representation schemas for the same logical resource URL without URL pollution.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q08ContentNegotiationVersioningExample.java"

---

## Senior Questions

### 9. How do you migrate legacy manual HttpServiceProxyFactory boilerplate from Spring Boot 3 to declarative client configurations in Spring Boot 4?

??? question "Reveal answer"
    In Spring Boot 3, developers had to write repetitive configuration beans creating `RestClient`, wrapping it in `RestClientAdapter.create(client)`, and invoking `HttpServiceProxyFactory.builderFor(adapter).build().createClient(...)`. In Spring Boot 4 / Framework 7, declarative clients can be registered via framework annotations and centralized client builders, ensuring uniform timeout, tracing, and observation configurations across all microservice clients.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q09MigrationHttpServiceProxyFactoryToBoot4Example.java"

### 10. How do you refactor an ad-hoc @RequestHeader API version check into declarative Spring Framework 7 route mappings?

??? question "Reveal answer"
    Ad-hoc `@RequestHeader("X-API-Version")` parameter checks inside controller methods lead to conditional spaghetti, cyclomatic bloat, and poor OpenAPI specs. Refactoring to Spring Framework 7 involves removing imperative `if/else` checks and using declarative header conditions (`headers = "X-API-Version=1"`, `headers = "X-API-Version=2"`) or dedicated versioned controller classes, with a centralized fallback returning RFC 9457 `400 Bad Request` for unsupported versions.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q10MigrationAdHocHeaderToNativeVersioningExample.java"

### 11. How should distributed idempotency keys be managed in high-concurrency virtual-thread REST APIs to avoid race conditions and duplicate operations?

??? question "Reveal answer"
    When thousands of virtual threads execute REST operations concurrently, non-idempotent endpoints (like `POST /charges`) must validate `Idempotency-Key` headers atomically. An atomic `SET NX EX` in Redis or `putIfAbsent` in local locks guarantees that only the first thread executes the mutation, while concurrent duplicate requests block or return the cached response, preventing double billing or duplicate state mutations.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q11IdempotentRestConsumerVirtualThreadsExample.java"

### 12. How do you implement timeout isolation and fallback mechanisms when calling external microservices via declarative HTTP interfaces?

??? question "Reveal answer"
    Declarative HTTP clients should combine socket timeouts with resilience patterns (circuit breakers, fallbacks, or bulkheads). When an external service exceeds its timeout threshold or returns persistent 5xx errors, the calling service catches the specific client timeout exception and provides a degraded response (e.g. cached reference data or an asynchronous acceptance 202) rather than propagating cascading failures to upstream users.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q12ResilientDeclarativeClientFallbacksExample.java"

---

## Scenario Questions

### 13. Scenario: An e-commerce platform decommissioned API v1 without RFC 8594 headers, causing 35% of mobile checkouts to fail. How does an RFC-compliant sunset strategy prevent outages?

??? question "Reveal answer"
    Abruptly turning off an API version breaks older mobile app clients that cannot be updated immediately by users. An RFC-compliant sunset strategy establishes a transparent multi-month lifecycle:
    1. **Deprecation Period**: V1 responses emit `Deprecation: @<timestamp>`, `Sunset: <date>`, and `Link: <url>; rel="deprecation"`.
    2. **Telemetry & Client Monitoring**: API gateways track incoming requests still sending `X-API-Version: 1` and notify impacted partners and app users.
    3. **Brownouts (Chaos Drills)**: Periodic intentional 15-minute simulated outages returning RFC 9457 `410 Gone` or `400 Bad Request` alerting users before hard decommissioning.
    4. **Final Cut-Off**: Seamless migration without surprise check-out failures.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/10-rest-api/src/examples/java/lab/java25boot4/restapi/questions/Q13ScenarioVersionSunsetOutageExample.java"
