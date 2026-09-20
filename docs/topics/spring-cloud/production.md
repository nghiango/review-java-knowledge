# Spring Cloud in Production

Operational failure modes, incident walkthroughs, key metrics, and a production readiness checklist for Spring Cloud systems.

---

## Production Incident 1: The Gateway Netty Event Loop Starvation

### Incident Timeline

| Time | Event |
|---|---|
| **14:00** | Deployment of Spring Cloud Gateway v2.4.0 containing a new `GeolocationFilter` to extract country data from IP addresses. |
| **14:05** | Downstream third-party IP geolocation API begins experiencing an elevated response latency of 1,500ms due to upstream packet loss. |
| **14:06** | Gateway P99 latency spikes from 12ms to 45 seconds across **all** customer endpoints (including unrelated auth and catalog routes). |
| **14:08** | Client connection timeouts proliferate. Edge ALB returns HTTP `504 Gateway Timeout` for 98% of all external traffic. Gateway CPU utilization paradoxically drops to near 0%. |
| **14:12** | SRE captures a thread dump (`jcmd <pid> Thread.print`) before rolling back to v2.3.0. Traffic recovers immediately. |

### Root Cause Analysis

Inspection of the thread dump revealed:

```text
"reactor-http-epoll-1" #32 daemon prio=5 os_prio=0 cpu=45.21ms elapsed=720s tid=0x00007f9c980 nid=0x4b waiting on condition
   java.lang.Thread.State: WAITING (parking)
        at jdk.internal.misc.Unsafe.park(Native Method)
        at java.util.concurrent.locks.LockSupport.park(LockSupport.java:221)
        at java.util.concurrent.CompletableFuture$Signaller.block(CompletableFuture.java:1864)
        at java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3780)
        at java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3725)
        at java.util.concurrent.CompletableFuture.waitingGet(CompletableFuture.java:1898)
        at java.util.concurrent.CompletableFuture.get(CompletableFuture.java:1992)
        at lab.springcloud.gateway.GeolocationFilter.filter(GeolocationFilter.java:38)
```

The developer implemented `GeolocationFilter` by creating a `CompletableFuture` and invoking `.get()` inside the gateway filter chain:

```java
// FATAL CODE: Blocking inside Netty event loop
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  CompletableFuture<GeoData> future = CompletableFuture.supplyAsync(() -> geoService.lookup(ip));
  GeoData data = future.get(); // BLOCKS reactor-http-epoll thread!
  exchange.getRequest().mutate().header("X-Country", data.country()).build();
  return chain.filter(exchange);
}
```

Because Reactor Netty allocates exactly 1 event loop per CPU core (8 cores = 8 threads), having 8 concurrent incoming requests hit `future.get()` froze all 8 event loops simultaneously. The gateway could not read incoming TCP packets, schedule timers, or process responses for any other route.

### Remediation & Preventive Measures

1. **Pure Reactive Flow**: Refactor the filter to use reactive composition:
   ```java
   public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
     return geoClient.lookupReactive(ip)
         .timeout(Duration.ofMillis(200))
         .onErrorReturn(GeoData.UNKNOWN)
         .flatMap(data -> {
           ServerHttpRequest mutated = exchange.getRequest().mutate()
               .header("X-Country", data.country())
               .build();
           return chain.filter(exchange.mutate().request(mutated).build());
         });
   }
   ```
2. **BlockHound CI Enforcement**: Add `BlockHound.install()` to gateway test suites to fail any unit or integration test that invokes blocking calls on non-blocking threads.

---

## Production Incident 2: The `@RefreshScope` Thundering Herd

### Incident Timeline

| Time | Event |
|---|---|
| **09:30** | SRE team updates feature flag configuration in Git and invokes `POST /actuator/busrefresh` on Spring Cloud Config Server. |
| **09:30:05** | All 40 instances of `OrderService` receive the `RefreshRemoteApplicationEvent` via Kafka bus. |
| **09:30:08** | Order service instances evict their `@RefreshScope` beans (including payment clients, pricing engines, and HikariCP data sources). |
| **09:30:10** | High-traffic checkout requests (8,000 req/sec) hit the refreshed services. Hundreds of concurrent threads on each pod attempt to re-instantiate `@RefreshScope` singletons simultaneously. |
| **09:30:15** | Intensive lock contention on `GenericScope` internal locks. JVM CPU spikes to 100%. HikariCP attempts to recreate 40 connection pools at once, overwhelming the PostgreSQL master database with 2,000 new TCP connections. |
| **09:30:30** | Database rejects connections (`FATAL: remaining connection slots are reserved for non-replication superuser connections`). Cascade outage occurs. |

### Root Cause Analysis

Dynamic reload via `@RefreshScope` clears bean caches across the entire cluster in lockstep. Under high throughput, this triggers a **thundering herd** where concurrent worker threads block on scope locks while eagerly rebuilding expensive objects (such as database pools and network clients).

### Remediation & Architectural Shift

1. **Avoid `@RefreshScope` for Heavy Infrastructure**: Never annotate `DataSource`, `HikariConfig`, or HTTP connection pool beans with `@RefreshScope`.
2. **Shift to Immutable GitOps Deployments**: Deprecate dynamic bus refresh in favor of Kubernetes rolling updates. When configuration changes in Git, ArgoCD rolls pods incrementally with zero downtime:
   - Pods are created one by one.
   - Readiness probes ensure the new pod is fully warmed up and healthy before receiving traffic.
   - Zero lock contention or database connection spikes on live traffic.

---

## Essential Metrics and Telemetry

| Metric Name | Type | Description & Alert Threshold |
|---|---|---|
| `spring.cloud.gateway.requests` | Counter / Timer | Total gateway throughput and routing latency tagged by `routeId` and `httpStatusCode`. Alert on 5xx rate > 1%. |
| `reactor.netty.connection.provider.active.connections` | Gauge | Active outbound pooled connections to backend services. Alert if > 80% of `max-connections`. |
| `reactor.netty.connection.provider.pending.connections` | Gauge | Requests waiting for an available connection from the pool. **Alert immediately if > 0** (indicates connection pool exhaustion). |
| `resilience4j.circuitbreaker.state` | Gauge | State of route/client circuit breakers (`0: CLOSED`, `1: OPEN`, `2: HALF_OPEN`). Alert on `OPEN` state. |
| `resilience4j.circuitbreaker.failure.rate` | Gauge | Percentage of failed calls in the sliding window. Alert if > 25%. |
| `feign.client.requests` | Timer | Latency and throughput of OpenFeign client invocations. Track P95 and P99 percentiles. |

---

## Production Readiness Checklist

### Spring Cloud Gateway
- [ ] Explicit global `connect-timeout` ($\le 1000\text{ms}$) and `response-timeout` ($\le 5\text{s}$) configured in `spring.cloud.gateway.httpclient`.
- [ ] Route-specific metadata timeouts configured for known slow or heavy endpoints.
- [ ] Outbound connection pool configured with explicit `max-connections` and `max-idle-time`.
- [ ] `RemoveRequestHeader` configured for all sensitive internal headers (`X-User-Id`, `X-Internal-Secret`) on public routes.
- [ ] Distributed rate limiting (`RequestRateLimiter`) active on all public unauthenticated routes.
- [ ] Circuit breaker filters (`CircuitBreaker`) configured with valid fallback endpoints on critical routes.
- [ ] Zero blocking calls (`block()`, `get()`, synchronous JDBC) executed on Netty event loops (verified with BlockHound).

### OpenFeign Clients
- [ ] Explicit `Request.Options` configured with bounded connect and read timeouts on every client.
- [ ] Blind retries disabled (`Retryer.NEVER_RETRY`).
- [ ] Mutating HTTP POST/PUT operations protected with client-supplied `Idempotency-Key` headers.
- [ ] Custom `ErrorDecoder` implemented to map 4xx client errors into domain exceptions instead of generic 500s.
- [ ] `FallbackFactory` defined to handle circuit breaker open states gracefully.
- [ ] Trace headers (`traceparent`) and authentication context propagated via `RequestInterceptor`.

### Cloud-Native Migration Assessment
- [ ] Evaluate if Eureka can be replaced with Kubernetes CoreDNS.
- [ ] Evaluate if dynamic `@RefreshScope` / Spring Cloud Bus can be replaced with GitOps and Kubernetes rolling updates.
- [ ] Evaluate if Spring Cloud LoadBalancer can be replaced with Kubernetes Service `ClusterIP` or Service Mesh.
- [ ] Evaluate if OpenFeign can be replaced with Spring 6 native `HttpInterfaces` / `RestClient`.
