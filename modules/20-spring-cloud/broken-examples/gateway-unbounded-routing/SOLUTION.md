# Solution — Spring Cloud Gateway Unbounded Routing

## Annotated code

```yaml
server:
  port: 8080

spring:
  application:
    name: edge-gateway
  cloud:
    gateway:
      # Resilience issue: Missing global Netty HTTP client connect and response timeouts.
      # By default, Reactor Netty HttpClient has no response timeout (infinite) and an unbounded or long connect timeout.
      # When a downstream microservice experiences high latency or hangs, pending gateway connections never complete,
      # consuming TCP sockets and Netty event loop memory until the edge gateway suffers total outage.
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            # Resilience issue: Route lacks CircuitBreaker filter and fallback endpoint.
            # If the order service crashes or degrades, all client traffic hits a failing backend,
            # triggering connection retries and cascading latency through the edge gateway.
            # Scalability issue: Route lacks rate limiting filter (RequestRateLimiter).
            # Clients can flood the gateway and overwhelm the backend order service without any token bucket throttling.
        - id: payment-service-route
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
          filters:
            - StripPrefix=1
            # Security issue: Hardcoded sensitive secret header in configuration file and missing client header sanitization.
            # Injecting static secrets in unencrypted yaml risks leakage, and without RemoveRequestHeader filters,
            # external callers can spoof internal authentication and tenant headers.
            - AddRequestHeader=X-Internal-Secret, super-secret-token
        - id: catalog-service-route
          uri: lb://catalog-service
          predicates:
            - Path=/api/catalog/**
          filters:
            - StripPrefix=1
            # Resilience issue: Missing route-specific response timeout configuration.
            # Catalog search can suffer from slow queries; without an explicit per-route metadata timeout,
            # catalog searches can hold edge gateway resources for minutes.
```

## Issue list

### Resilience issue: Missing global Netty HTTP client connect and response timeouts

- **Location:** `application.yml:7`
- **Description:** Spring Cloud Gateway relies on Project Reactor Netty's `HttpClient`. Without configuring `spring.cloud.gateway.httpclient.connect-timeout` and `spring.cloud.gateway.httpclient.response-timeout`, downstream requests can block indefinitely on unresponsive connections.
- **Impact:** Slow or hanging backend services tie up Netty connection pool slots and socket descriptors, cascading downstream degradation into total gateway failure.
- **Remediation:** Configure explicit global connect (e.g. 1000ms) and response (e.g. 5000ms) timeouts under `spring.cloud.gateway.httpclient`.

### Resilience issue: Routes lack CircuitBreaker filter and fallback endpoint

- **Location:** `application.yml:15`
- **Description:** Routes forward traffic directly to `lb://order-service` without a Spring Cloud CircuitBreaker / Resilience4j filter.
- **Impact:** Repeated failures or severe degradation on downstream services cause gateway requests to queue and fail with raw 500/504 errors rather than failing fast or returning graceful degraded fallback responses.
- **Remediation:** Attach the `CircuitBreaker` filter (`name: orderCircuitBreaker`, `fallbackUri: forward:/fallback/orders`) to each downstream route.

### Scalability issue: Missing rate limiting filter on public edge routes

- **Location:** `application.yml:15`
- **Description:** Public edge endpoints accept arbitrary request volumes without `RequestRateLimiter` filters.
- **Impact:** A single client or distributed botnet can saturate downstream microservices with unthrottled requests, causing cascading outages across the cluster.
- **Remediation:** Configure `RequestRateLimiter` with Redis token bucket algorithm (`redis-rate-limiter.replenishRate`, `redis-rate-limiter.burstCapacity`) and a client key resolver (e.g., API key or authenticated user ID).

### Security issue: Static secret token hardcoded in route filter without client header stripping

- **Location:** `application.yml:25`
- **Description:** A static internal authentication secret is hardcoded in the gateway configuration, and client-supplied headers are not stripped.
- **Impact:** Secrets in source control can be compromised, and external attackers can inject or forge internal routing and authorization headers (`X-Internal-Secret`, `X-User-Id`) unless explicitly stripped before adding gateway-managed headers.
- **Remediation:** Externalize secrets using secret managers or environment variables, and add `RemoveRequestHeader` filter to strip untrusted incoming headers prior to routing.

## Correct implementation

See [`correct/application.yml`](correct/application.yml).

Detailed discussion in [Solutions](../../../docs/topics/spring-cloud/solutions.md#edge-gateway-timeouts-resilience-and-rate-limiting).
