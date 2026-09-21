# REST API Hands-On Exercises — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline exercises in [`docs/topics/rest-api/exercises.md`](../../../topics/rest-api/exercises.md) cover implementing idempotency filters, ETag handlers, and RFC 9457 custom problem detail resolvers.
    These exercises focus on **Spring Boot 4.0 / Spring Framework 7 & Java 25 REST skills**:

---

## Exercise 1: Refactor Imperative Version Dispatching to Declarative Header Mapping

### Objective
Given a legacy controller that parses `X-API-Version` inside the method body:
1. Break down the method into separate, dedicated `@GetMapping` handler methods.
2. Configure header matching (`headers = "X-API-Version=1"`, `headers = "X-API-Version=2"`).
3. Introduce an explicit fallback returning an RFC 9457 `ProblemDetail` with status `400 Bad Request` listing all supported API versions.
4. Attach RFC 8594 `Sunset` and `Deprecation` response headers to version 1.

### Verification Kata
Write a unit test with MockMvc or `RestTestClient` verifying that:
- Request with header `X-API-Version: 1` returns status 200 and headers `Sunset` and `Deprecation`.
- Request with header `X-API-Version: 2` returns status 200 without `Sunset`.
- Request with header `X-API-Version: 99` returns status 400 with title `"Unsupported API Version"`.

---

## Exercise 2: Build a Resilient Declarative HTTP Client with ProblemDetail Decoding

### Objective
Create an `@HttpExchange` client interface for an external Shipping & Logistics API:
1. Define `@HttpExchange("/api/v1/shipments")` with methods for creating and tracking shipments.
2. Configure `SimpleClientHttpRequestFactory` with a 1500ms connect timeout and 2500ms read timeout.
3. Configure a `defaultStatusHandler` on the `RestClient` that converts downstream HTTP 4xx/5xx responses into a custom `DownstreamProblemException` holding the parsed RFC 9457 `ProblemDetail`.
4. Verify using a unit test that exceptions thrown by the client preserve the downstream problem detail properties.
