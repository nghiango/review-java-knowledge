# Production Incidents & Runbooks — REST APIs (Java 25 & Spring Boot 4)

!!! info "Delta from baseline"
    Baseline production scenarios in [`docs/topics/rest-api/production.md`](../../../topics/rest-api/production.md) cover duplicate checkout retries, unbounded collection memory exhaustion, and ETag lost updates.
    This page covers **production scenarios specific to Spring Boot 4.0 / Spring Framework 7 REST systems**:
    
    1. **The Sudden API Decommissioning Outage**: What happens when legacy endpoints are killed without RFC 8594 communication.
    2. **The Declarative HTTP Client Hanging Thread Avalanche**: How unconfigured socket timeouts in `@HttpExchange` bring down upstream virtual threads.

---

## Incident 1: Uncoordinated API Version Sunset Outage

### Incident Timeline & Symptoms
- **09:15 UTC**: A backend team removes support for `OrderDto` v1 from the API Gateway to enforce the new schema.
- **09:18 UTC**: Mobile checkout traffic drops by 35%. Millions of mobile app users on iOS/Android v2.4 (which hardcoded API v1) receive generic HTTP 500 or 404 errors.
- **09:30 UTC**: Emergency rollback initiated.

### Root Cause
The API was deprecated internally in Slack channels, but no machine-readable RFC 8594 headers (`Sunset`, `Deprecation`, `Link`) were ever emitted in API responses. Neither automated client SDK monitors nor partner engineering teams had visibility into the decommissioning deadline.

### Remediation & Runbook
1. **Enforce RFC 8594 Headers**: Require all deprecated endpoints to return `Deprecation: @<epoch>` and `Sunset: <date>`.
2. **Prometheus / Micrometer Version Metrics**:
   ```java
   Metrics.counter("http.server.requests.version", "version", requestedVersion, "status", status).increment();
   ```
3. **Automated Deprecation Dashboard**: Trigger PagerDuty alerts if traffic to deprecated endpoints exceeds 2% within 30 days of the `Sunset` date.
4. **Brownout Testing**: Implement 15-minute scheduled brownouts 60 days, 30 days, and 7 days prior to hard shutdown.

---

## Incident 2: Declarative HTTP Client Virtual Thread Socket Stall

### Incident Timeline & Symptoms
- **14:00 UTC**: A third-party fraud scoring provider suffers a silent network degradation, accepting TCP connections but taking 60+ seconds to respond.
- **14:03 UTC**: The payment service thread count skyrockets from 200 to 45,000 virtual threads.
- **14:05 UTC**: JVM memory rises sharply due to unmounted thread stack frames and connection buffer allocations; the service encounters `OutOfMemoryError: unable to create native thread / file descriptors exhausted`.

### Root Cause
The newly introduced `@HttpExchange` payment client was created using `RestClient.builder().baseUrl(...)` without setting socket read and connect timeouts. Under high traffic, thousands of concurrent virtual threads stalled on socket reads indefinitely.

### Prevention & Verification
Always configure bounded timeouts on `SimpleClientHttpRequestFactory` or `JdkClientHttpRequestFactory`:
```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(1000); // 1 second
factory.setReadTimeout(2000);    // 2 seconds
```
Combine with Resilience4j circuit breakers or rate limiters to fast-fail when error rates spike.
