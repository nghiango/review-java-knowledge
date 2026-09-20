# Spring MVC in Production

Production operations, incident diagnosis, security hardening, and performance best practices for Spring MVC applications.

## 1. Diagnosing Tomcat HTTP Thread Pool Exhaustion

Under sudden downstream latency spikes, Spring MVC applications running on default embedded Tomcat configurations can quickly exhaust the worker thread pool (default 200 threads).

### Incident Symptoms
- Incoming HTTP requests encounter connection timeouts or `504 Gateway Timeout` errors.
- Kubernetes liveness/readiness probes fail because health check endpoints cannot acquire an HTTP worker thread.
- Thread dumps reveal hundreds of Tomcat threads in `http-nio-8080-exec-*` in `WAITING` or `TIMED_WAITING` state on downstream I/O.

### Remediation Strategy
1. **Configure Strict Timeouts**: Set aggressive connect and read timeouts on `RestClient` and `WebClient` instances.
2. **Asynchronous Offloading**: Offload slow, long-running operations (reporting, large file generation, multi-stage aggregation) to `DeferredResult<T>` or `CompletableFuture<T>` running on bounded, dedicated thread pools (`ThreadPoolTaskExecutor`).
3. **Resilience & Circuit Breaking**: Apply Resilience4j circuit breakers and rate limiters to fail fast when downstream services degrade.

---

## 2. Preventing Sensitive Information Leakage in API Errors

Default exception handling mechanisms in development environments often include raw stack traces, database table names, SQL query strings, and internal class hierarchies.

### Security Risks
- Attackers exploit detailed SQL errors to map database schemas and craft targeted SQL injection payloads.
- Stack traces reveal exact dependency versions, open-source library vulnerabilities, and internal architecture.

### Remediation Strategy
1. **Centralized `@RestControllerAdvice`**: Intercept all unhandled exceptions globally.
2. **RFC 9457 `ProblemDetail`**: Map exceptions to standard `ProblemDetail` structures with generic client messages (e.g. "An internal server error occurred").
3. **Internal Logging**: Log full stack traces and correlation IDs internally at `ERROR` level while returning only a correlation/trace ID to the client.

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An internal server error occurred. Reference ID: trace-9012",
  "instance": "/api/v1/orders/123"
}
```

---

## 3. Hardening CORS Configuration in Production

Cross-Origin Resource Sharing (CORS) misconfigurations are a common vulnerability in enterprise single-page applications (SPAs).

### Dangerous Anti-Patterns
- `allowedOriginPatterns("*")` paired with `allowCredentials(true)` allows any malicious origin to send authenticated requests on behalf of logged-in users.
- Reflecting the incoming `Origin` request header blindly into the `Access-Control-Allow-Origin` response header.

### Production Best Practices
1. Maintain an explicit whitelist of authorized frontend origins in configuration properties.
2. Restrict allowed HTTP methods to only those actually supported by the API.
3. Set `maxAge(3600)` on CORS mappings to cache preflight `OPTIONS` responses and avoid unnecessary preflight roundtrips.

---

## 4. Production Spring MVC Checklist

- [ ] All request DTOs declare Bean Validation constraints (`@NotBlank`, `@Email`, `@Min`, `@Size`) and controllers declare `@Valid`.
- [ ] Centralized `@RestControllerAdvice` handles exceptions and returns RFC 9457 `ProblemDetail` without raw stack traces.
- [ ] Controller endpoints return semantic HTTP status codes (`201 Created` with `Location` header, `204 No Content`, `400 Bad Request`, `404 Not Found`).
- [ ] Controllers are thin orchestrators: zero business logic, zero pricing algorithms, and zero direct entity mutations.
- [ ] Long-running operations use `DeferredResult` or `CompletableFuture` on isolated thread pools.
- [ ] CORS policies use explicit origin whitelists; never combine wildcard origins with credentials.
- [ ] Singleton controllers are completely stateless with no shared mutable instance variables.
