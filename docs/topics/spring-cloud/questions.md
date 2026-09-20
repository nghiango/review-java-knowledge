# Spring Cloud Interview Questions

Core interview questions covering Spring Cloud Gateway, OpenFeign, Config Server, Service Discovery, Load Balancing, Resilience, and the Cloud-Native evolution to Kubernetes and AWS.

<!-- --8<-- [start:basic] -->
## Basic

### What is the primary role of an API Gateway in a microservice architecture?

??? question "Reveal answer"
    **Short Answer:** An API Gateway serves as the single entry point for external client traffic, decoupling clients from internal microservice boundaries. It provides cross-cutting operational concerns such as request routing, SSL/TLS termination, centralized authentication and JWT validation, distributed rate limiting, circuit breaking, path rewriting, and unified telemetry/metrics collection, preventing individual microservices from duplicating edge logic.

    ??? example "Example"
        ```yaml
        spring:
          cloud:
            gateway:
              routes:
                - id: order-service
                  uri: lb://order-service
                  predicates:
                    - Path=/api/v1/orders/**
        ```

### How does Spring Cloud Gateway differ architecturally from legacy Netflix Zuul 1.x?

??? question "Reveal answer"
    **Short Answer:** Netflix Zuul 1.x was built on the blocking Java Servlet API, utilizing a "thread-per-request" model where each incoming connection occupies a dedicated worker thread throughout downstream network I/O. Spring Cloud Gateway is built natively on Spring WebFlux, Project Reactor, and Netty, using non-blocking asynchronous event loops (typically 1 thread per CPU core). A single Spring Cloud Gateway instance can maintain tens of thousands of concurrent open connections with minimal memory and thread overhead, avoiding thread pool starvation during downstream latency spikes.

    ??? example "Example"
        ```mermaid
        flowchart LR
            Zuul["Zuul 1.x: 200 Worker Threads (Blocking Servlet)"] -.->|Exhausted on Latency| Stall["Thread Pool Depletion"]
            SCG["Spring Cloud Gateway: 8 Event Loops (Netty Reactive)"] -->|Async Sockets| Scalable["Tens of Thousands Concurrent Connections"]
        ```

### What are Route Predicates and Gateway Filters in Spring Cloud Gateway?

??? question "Reveal answer"
    **Short Answer:** A **Route Predicate** is a functional condition evaluated against incoming HTTP request attributes (e.g. `Path`, `Method`, `Header`, `Query`, `Host`, `RemoteAddr`). If the predicate returns `true`, the route is matched. A **Gateway Filter** is an interceptor applied in a pipeline before forwarding the request downstream (Pre-filter) or before returning the response to the client (Post-filter), used to modify headers, strip prefixes, enforce rate limits, or apply circuit breakers.

    ??? example "Example"
        ```yaml
        spring:
          cloud:
            gateway:
              routes:
                - id: payment-route
                  uri: lb://payment-service
                  predicates:
                    - Path=/payments/**
                    - Method=POST
                  filters:
                    - StripPrefix=1
                    - AddRequestHeader=X-Gateway-Processed, true
        ```

### What is OpenFeign and what problem does it solve in Spring microservices?

??? question "Reveal answer"
    **Short Answer:** OpenFeign is a declarative HTTP client binder. Instead of manually writing repetitive HTTP request building, JSON serialization/deserialization, and error handling code using `RestTemplate` or `HttpClient`, developers define a Java interface with Spring MVC annotations (`@GetMapping`, `@PostMapping`). At runtime, Spring Cloud generates a dynamic proxy implementation that handles URL construction, parameter encoding, service discovery resolution, and response deserialization automatically.

    ??? example "Example"
        ```java
        @FeignClient(name = "catalog-service")
        public interface CatalogClient {
          @GetMapping("/api/items/{id}")
          ItemDto getItem(@PathVariable("id") String id);
        }
        ```

### What is Spring Cloud Config Server and how does it load configuration?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Config Server is a centralized HTTP and resource-based API for externalized application properties across environments (dev, test, prod). It pulls configuration from version-controlled backends like Git, HashiCorp Vault, or filesystem repositories. When a client microservice boots up, its Spring Cloud Config Client queries the Config Server (`GET /{application}/{profile}`) to populate its Spring `Environment` property sources before instantiating application beans.

    ??? example "Example"
        ```yaml
        # Config Client bootstrap
        spring:
          application:
            name: order-service
          config:
            import: "configserver:http://config-server:8888"
        ```

### What is Netflix Eureka and how does client-side service registration work?

??? question "Reveal answer"
    **Short Answer:** Netflix Eureka is a service registry and discovery server designed around the AP (Availability / Partition tolerance) model. Microservice instances register their network IP and port with the Eureka server on boot (`eureka.client.register-with-eureka=true`) and send periodic heartbeats (default every 30 seconds) to maintain their lease. Calling microservices fetch the registry cache periodically, allowing client-side load balancers to select active instances without central bottlenecks.

    ??? example "Example"
        ```yaml
        eureka:
          client:
            service-url:
              defaultZone: http://eureka-server:8761/eureka/
            lease-renewal-interval-in-seconds: 10
        ```

### What is client-side load balancing, and how does it contrast with server-side load balancing?

??? question "Reveal answer"
    **Short Answer:** In **server-side load balancing** (e.g. AWS ALB, NGINX), clients send traffic to a single virtual IP/DNS, and an intermediate hardware/software proxy chooses the target backend. In **client-side load balancing** (e.g. Spring Cloud LoadBalancer), the calling client maintains a local cache of available backend instances obtained from service discovery, executes a local balancing algorithm (Round-Robin, Random, Weighted), and makes a direct TCP/HTTP connection to the chosen target pod, eliminating the network hop of a middle proxy.

    ??? example "Example"
        ```mermaid
        flowchart LR
            subgraph ServerSide["Server-Side Load Balancing"]
                C1["Client"] --> Proxy["Hardware/Software Proxy (ALB)"]
                Proxy --> S1["Server A"]
                Proxy --> S2["Server B"]
            end
            subgraph ClientSide["Client-Side Load Balancing"]
                C2["Client (holds registry)"] -->|Direct TCP Connection| S3["Server A"]
            end
        ```

### What is Spring Cloud LoadBalancer, and which legacy library did it replace?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud LoadBalancer is the reactive, non-blocking client-side load balancing abstraction introduced by the Spring Cloud team to replace the legacy, deprecated **Netflix Ribbon**. Ribbon was blocking, tightly coupled to Apache HttpClient, and had been put into maintenance mode by Netflix. Spring Cloud LoadBalancer integrates natively with Project Reactor, Spring WebFlux, and modern non-blocking discovery suppliers.

    ??? example "Example"
        ```java
        @Configuration
        public class WebClientConfig {
          @Bean
          @LoadBalanced
          public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
          }
        }
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### Why is blocking code inside Spring Cloud Gateway filters or routes considered fatal?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Gateway runs on Netty Event Loops, where only a handful of threads (typically 1 per CPU core) handle thousands of concurrent client sockets. If a developer invokes blocking operations—such as `Mono.block()`, `Thread.sleep()`, synchronous JDBC calls, or blocking HTTP requests—inside a GatewayFilter, the entire event loop thread is frozen. All other client connections multiplexed onto that same event loop stop processing, leading to rapid latency spikes, connection timeouts, and total gateway paralysis across unrelated routes.

    ??? example "Example"
        ```java
        // DANGEROUS: Freezes Netty event loop thread!
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          // Blocking call inside reactive event loop:
          UserDto user = userClient.getUserSync(userId); // NEVER DO THIS
          return chain.filter(exchange);
        }
        ```

### How do you configure global and per-route timeouts in Spring Cloud Gateway?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Gateway uses Reactor Netty `HttpClient`. Global connect timeout and response timeouts are configured under `spring.cloud.gateway.httpclient.connect-timeout` (milliseconds) and `spring.cloud.gateway.httpclient.response-timeout` (Duration). Per-route response and connect timeouts can be overridden inside route metadata using `metadata.response-timeout` and `metadata.connect-timeout`, allowing slow reporting endpoints to tolerate longer processing without exposing fast payment endpoints to connection hangs.

    ??? example "Example"
        ```yaml
        spring:
          cloud:
            gateway:
              httpclient:
                connect-timeout: 1000
                response-timeout: 3s
              routes:
                - id: slow-report-route
                  uri: lb://report-service
                  predicates:
                    - Path=/api/reports/**
                  metadata:
                    response-timeout: 30000 # 30 seconds for reports
        ```

### How does OpenFeign handle HTTP 4xx and 5xx errors by default, and how should a custom `ErrorDecoder` be implemented?

??? question "Reveal answer"
    **Short Answer:** By default, OpenFeign's `ErrorDecoder.Default` translates non-2xx responses into generic runtime exceptions like `FeignException.BadRequest` (400), `FeignException.NotFound` (404), or `FeignException.InternalServerError` (500). This obscures business logic and causes 4xx errors to bubble up as 500s. A production `ErrorDecoder` inspects the HTTP status code, parses downstream error payloads (e.g. RFC 9457 `ProblemDetail`), and translates 404 into domain `EntityNotFoundException`, 409 into `ConflictException`, and marks transient 503/504 errors as `RetryableException`.

    ??? example "Example"
        ```java
        public class CustomErrorDecoder implements ErrorDecoder {
          @Override
          public Exception decode(String methodKey, Response response) {
            return switch (response.status()) {
              case 404 -> new ResourceNotFoundException("Resource not found");
              case 409 -> new BusinessConflictException("Conflict detected");
              case 503 -> new RetryableException(
                  response.status(), "Downstream unavailable",
                  response.request().httpMethod(), null, response.request());
              default -> new FeignException.InternalServerError(
                  "Downstream error: " + response.status(), response.request(), null, null);
            };
          }
        }
        ```

### Why should you avoid blind retries in OpenFeign configuration?

??? question "Reveal answer"
    **Short Answer:** OpenFeign's `Retryer.Default` retries on any `RetryableException` without differentiating between idempotent GET requests and state-mutating POST/PUT requests. If a network socket read timeout occurs on a POST checkout request, the server may have already charged the credit card. Retrying blindly causes duplicate charges or double reservations. Furthermore, multiple clients retrying against a struggling backend create an amplified **retry storm**. Retries should be disabled in Feign (`Retryer.NEVER_RETRY`) and handled explicitly with idempotency keys using Resilience4j decorators.

    ??? example "Example"
        ```java
        @Bean
        public Retryer feignRetryer() {
          // Disable blind retries in Feign client
          return Retryer.NEVER_RETRY;
        }
        ```

### How does `@RefreshScope` work internally in Spring Cloud, and what creates the proxy?

??? question "Reveal answer"
    **Short Answer:** `@RefreshScope` is a custom Spring bean scope (`GenericScope`). Beans annotated with `@RefreshScope` are registered with `ScopedProxyMode.TARGET_CLASS` and wrapped in CGLIB dynamic subclass proxies. When application code invokes a method on the bean, the proxy intercepts the call and delegates to a cached target instance managed by `RefreshScope`. When a refresh event occurs (`POST /actuator/refresh`), `RefreshScope` evicts all cached target instances. The next method invocation creates a new target instance bound to the updated `Environment` properties.

    ??? example "Example"
        ```mermaid
        sequenceDiagram
            Caller->>CGLIBProxy: execute()
            CGLIBProxy->>RefreshScope: getTarget()
            RefreshScope-->>CGLIBProxy: returns cached bean
            Note over RefreshScope: /actuator/refresh clears cache!
            Caller->>CGLIBProxy: execute()
            CGLIBProxy->>RefreshScope: getTarget()
            RefreshScope->>RefreshScope: Instantiate new bean with new properties
            RefreshScope-->>CGLIBProxy: returns new bean
        ```

### What is Spring Cloud Bus and how does it broadcast configuration changes?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Bus links nodes of a distributed system with a shared message broker (typically RabbitMQ or Apache Kafka). Instead of sending a `POST /actuator/refresh` request to every single microservice instance in a cluster, an operator or CI/CD webhook triggers `POST /actuator/busrefresh` on a single node. That node publishes a `RefreshRemoteApplicationEvent` to the message broker topic/exchange, and all subscribed microservices consume the event and refresh their local `@RefreshScope` beans simultaneously.

    ??? example "Example"
        ```text
        Git Webhook -> POST /actuator/busrefresh on Gateway
                             |
                             v
                    RabbitMQ / Kafka Bus
                     /       |       \
                    v        v        v
                 Order-1   Order-2  Payment-1
               (All nodes refresh @RefreshScope concurrently)
        ```

### How does Redis-backed rate limiting work in Spring Cloud Gateway?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Gateway provides `RequestRateLimiterGatewayFilterFactory` using the **Token Bucket algorithm** backed by Redis Lua scripts (`request_rate_limiter.lua`). For each incoming request, a `KeyResolver` determines the client identity (e.g. JWT user ID, API key, or client IP). The Lua script executes atomically in Redis, replenishing tokens based on elapsed time (`replenishRate`) and consuming tokens up to `burstCapacity`. If sufficient tokens exist, the request is allowed; otherwise, the gateway immediately returns HTTP `429 Too Many Requests`.

    ??? example "Example"
        ```yaml
        filters:
          - name: RequestRateLimiter
            args:
              redis-rate-limiter.replenishRate: 50
              redis-rate-limiter.burstCapacity: 100
              key-resolver: "#{@apiKeyResolver}"
        ```

### How do you integrate Circuit Breakers into Spring Cloud Gateway routes?

??? question "Reveal answer"
    **Short Answer:** Spring Cloud Gateway integrates with Spring Cloud CircuitBreaker and Resilience4j via the `CircuitBreaker` gateway filter (`SpringCloudCircuitBreakerFilterFactory`). You declare the filter with a circuit breaker configuration name and an optional `fallbackUri` (e.g. `forward:/fallback/orders`). If downstream calls exceed failure rate or slow call rate thresholds, the circuit breaker transitions to `OPEN` and immediately routes subsequent incoming requests to the internal fallback endpoint, preventing downstream cascading failure.

    ??? example "Example"
        ```yaml
        filters:
          - name: CircuitBreaker
            args:
              name: orderCircuitBreaker
              fallbackUri: forward:/fallback/orders
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### Why is running Netflix Eureka inside a Kubernetes cluster considered an anti-pattern?

??? question "Reveal answer"
    **Short Answer:** Running Eureka inside Kubernetes creates architectural redundancy, synchronization drift, and wasted resources. Kubernetes already provides native, bulletproof service discovery via **CoreDNS** and Layer 4 load balancing via `kube-proxy` or eBPF (Cilium). Adding Eureka introduces:
    1. **Split Health State**: Kubernetes knows instantly when a pod dies via kubelet container status and readiness probes. Eureka relies on client heartbeats (30s) and registry polling (30s), meaning Eureka's cache can be stale for up to 60–90 seconds, routing traffic to terminated pods and generating 502/504 errors.
    2. **JVM Footprint**: Running a dedicated, clustered Eureka server consumes significant memory and CPU purely for a registry that Kubernetes already provides in kernel/DNS.
    3. **Operational Complexity**: Eureka self-preservation mode during network partitions can freeze deregistration of failed pods indefinitely.

    ??? example "Example"
        ```text
        Pod Termination Timeline:
        T0: Pod receives SIGTERM (K8s removes pod from Endpoints immediately).
        T1: Traffic via K8s Service stops instantly (0ms latency).
        T2: Eureka still reports Pod as UP for 30-90s until lease expires!
        T3: Clients calling via Eureka receive Connection Refused (502).
        ```

### How should you sanitize incoming client headers in Spring Cloud Gateway to prevent internal header spoofing?

??? question "Reveal answer"
    **Short Answer:** In a microservice mesh, downstream services often trust headers injected by the edge gateway (e.g. `X-User-Id`, `X-User-Roles`, `X-Internal-Secret`, `X-Tenant-Id`). If the gateway does not explicitly strip incoming client headers, a malicious external attacker can craft an HTTP request containing `X-User-Id: admin-001` or `X-Internal-Secret: bypass`, impersonating privileged users. The gateway must configure default filters (`RemoveRequestHeader`) at the global level to sanitize all internal header names from incoming requests *before* downstream authentication filters append verified values.

    ??? example "Example"
        ```yaml
        spring:
          cloud:
            gateway:
              default-filters:
                - RemoveRequestHeader=X-User-Id
                - RemoveRequestHeader=X-User-Roles
                - RemoveRequestHeader=X-Internal-Secret
        ```

### What are the operational failure modes and risks of using `@RefreshScope` in high-throughput production services?

??? question "Reveal answer"
    **Short Answer:** While `@RefreshScope` provides dynamic configuration updates without JVM restarts, it introduces several severe production hazards:
    1. **Connection Pool Rebirth / Leakage**: Rebinding beans like HikariCP or HTTP connection pools dynamically can leave old sockets unclosed or cause sudden connection spikes to downstream databases.
    2. **Cluster Inconsistency**: During rolling config refresh, different nodes in the cluster run with different configuration values for minutes, leading to split-brain business logic (e.g. differing tax rates or pricing rules).
    3. **Thread Safety & Latency Spikes**: The eviction of cached instances forces multiple concurrent worker threads to compete on re-instantiating beans, causing thread contention and latency spikes.
    4. **Senior Recommendation**: Rely on standard immutable deployments via GitOps (ArgoCD) and Kubernetes rolling updates instead of JVM dynamic refresh. When configuration changes, roll new pods; Kubernetes handles zero-downtime rolling deploys cleanly and deterministically.

    ??? example "Example"
        ```mermaid
        flowchart TD
            Refresh["POST /actuator/refresh"] --> Evict["Evict Bean Instances"]
            Evict --> Storm["100 Concurrent Threads Hit CGLIB Proxy"]
            Storm --> Lock["TargetSource Re-instantiation Lock"]
            Lock --> Spike["P99 Latency Spikes from 10ms to 2000ms"]
        ```

### How do you pass security contexts, distributed tracing headers, and tenant metadata across OpenFeign calls in Java 21?

??? question "Reveal answer"
    **Short Answer:** Cross-cutting metadata must be propagated via Feign `RequestInterceptor` beans. For W3C tracecontext (`traceparent`) and Micrometer Observation, Spring Cloud OpenFeign auto-configures propagation using Micrometer's `ObservationRegistry`. For security tokens (JWT) and tenant IDs, a custom `RequestInterceptor` extracts the token from the current `SecurityContextHolder` or request attributes and appends it as an `Authorization: Bearer <token>` or `X-Tenant-Id` header. In Java 21 with Virtual Threads, ensure the context holder uses `InheritableThreadLocal` or Spring Security's virtual thread context propagation mode to prevent context loss across task handoffs.

    ??? example "Example"
        ```java
        @Component
        public class FeignAuthInterceptor implements RequestInterceptor {
          @Override
          public void apply(RequestTemplate template) {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
              String authHeader = attributes.getRequest().getHeader("Authorization");
              if (authHeader != null) {
                template.header("Authorization", authHeader);
              }
            }
          }
        }
        ```

### When should an engineering team choose Spring 6 `HttpInterfaces` / `RestClient` over OpenFeign in new Spring Boot 3.5 projects?

??? question "Reveal answer"
    **Short Answer:** In modern Spring Boot 3.5+ projects, **Spring 6 HTTP Interfaces** (`@HttpExchange`, `@GetExchange`) backed by `RestClient` (or `WebClient`) should be preferred for all new microservice architectures unless legacy Feign-specific extensions are strictly required.
    1. **Zero Extra Dependencies**: HTTP Interfaces are built into core `spring-web`, eliminating the need for `spring-cloud-starter-openfeign` and its heavy Netflix legacy dependencies.
    2. **Modern HTTP Engines**: Natively works with `RestClient` and modern JDK 21 `HttpClient` or Apache HttpComponents 5, supporting HTTP/2 seamlessly.
    3. **First-Class Observability**: Built-in integration with Micrometer Observation and OpenTelemetry tracing without custom Feign capability bridges.
    4. **Compile-Time Safety**: Clean interfaces that do not rely on dynamic reflection hacks or older ribbon hooks.

    ??? example "Example"
        ```java
        // Modern Spring Boot 3.5 Declarative Client
        @HttpExchange("/api/v1/orders")
        public interface OrderClient {
          @GetExchange("/{id}")
          OrderDto getOrder(@PathVariable("id") String id);
        }

        @Bean
        public OrderClient orderClient(RestClient.Builder builder) {
          RestClient restClient = builder.baseUrl("http://order-service").build();
          HttpServiceProxyFactory factory =
              HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
          return factory.createClient(OrderClient.class);
        }
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
### Incident: An audit logging filter blocks a Netty event loop thread in Spring Cloud Gateway, freezing all routes. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** In Spring Cloud Gateway, Netty allocates only 1 event loop thread per CPU core (e.g. 8 threads total). A developer added an audit filter making a synchronous blocking call via `RestTemplate.postForObject()`. When the audit service slowed down to 2-second response latency, all 8 Netty event loop threads were simultaneously blocked. Because event loops are multiplexed across thousands of client connections, the entire gateway froze, rejecting or timing out all incoming requests across all unrelated microservices.

    **Remediation:**
    1. **Asynchronous Non-Blocking Execution**: Replace `RestTemplate` with `WebClient` and compose the reactive stream without calling `.block()`.
    2. **Decoupled Asynchronous Offload**: Emit audit records to an asynchronous message broker (Kafka/RabbitMQ) or offload the work using `.publishOn(Schedulers.boundedElastic())`.
    3. **Automated Detection with BlockHound**: Add `BlockHound` to the test suite to automatically fail builds if blocking I/O is invoked on non-blocking threads.

    ??? example "Example"
        ```java
        // Non-blocking reactive gateway filter
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
          return auditWebClient.post()
              .uri("/log")
              .bodyValue(new AuditRecord(userId, exchange.getRequest().getPath().value()))
              .retrieve()
              .toBodilessEntity()
              .timeout(Duration.ofMillis(200))
              .onErrorResume(e -> Mono.empty()) // Degrade gracefully on audit failure
              .then(chain.filter(exchange));
        }
        ```

### Incident: OpenFeign default 60-second read timeouts and blind retries cause connection starvation and retry storm. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** `OrderService` called `InventoryService` via OpenFeign with default 60-second read timeouts and `Retryer.Default(100, 1000, 5)`. When `InventoryService` slowed down due to database contention, calling Tomcat worker threads were blocked for up to 60 seconds each, rapidly saturating the 200-thread pool. Furthermore, whenever calls timed out, Feign blindly fired up to 5 additional attempts for every failed request, generating a 5x retry storm that completely crashed the struggling inventory backend.

    **Remediation:**
    1. **Tight Multi-Layer Deadlines**: Configure `Request.Options` with explicit 500ms connect and 2000ms read timeouts.
    2. **Eliminate Feign-Level Blind Retries**: Set `Retryer.NEVER_RETRY` in Feign configuration.
    3. **Resilience4j Circuit Breaking**: Wrap inventory calls in a Circuit Breaker with a 50% failure rate threshold. When inventory slows down, the circuit trips to `OPEN` and fails fast in 0ms, protecting both services.
    4. **Idempotency Enforcement**: Ensure state-mutating requests carry unique `Idempotency-Key` headers.

    ??? example "Example"
        ```java
        @Bean
        public Request.Options feignOptions() {
          return new Request.Options(500, TimeUnit.MILLISECONDS, 2000, TimeUnit.MILLISECONDS, true);
        }

        @Bean
        public Retryer feignRetryer() {
          return Retryer.NEVER_RETRY; // Disable blind retries; use Resilience4j with backoff & jitter
        }
        ```
<!-- --8<-- [end:scenarios] -->
