# Interview Checklist

## Core Java

- [ ] Explain OOP, SOLID and composition versus inheritance with trade-offs
- [ ] Define the `equals` / `hashCode` contract and identify mutable-key failures
- [ ] Explain HashMap hashing, collisions, resize and treeification
- [ ] Compare records, immutable classes, sealed types and enums
- [ ] Apply PECS and explain type erasure
- [ ] Distinguish checked and unchecked exceptions and use try-with-resources
- [ ] Use Optional only where absence is part of a return contract
- [ ] Explain stream laziness, stateful operations and collectors
- [ ] Identify unsafe stream side effects and parallel-stream misuse
- [ ] Discuss Java 21 pattern matching and sequenced collections

## JVM & Performance

- [ ] Explain JVM runtime data areas, heap generations, Metaspace and stack frames
- [ ] Trace class loading lifecycle, parent delegation and class initialization triggers
- [ ] Explain JIT tiered compilation (C1, C2), deoptimization and escape analysis
- [ ] Contrast G1, ZGC, Shenandoah and Parallel GC with pause-time and throughput trade-offs
- [ ] Identify GC roots, safepoints, card tables and remembered sets
- [ ] Diagnose `OutOfMemoryError` variants (Heap, Metaspace, Direct, Native Threads)
- [ ] Analyze heap dump dominator trees and JFR allocation profiles
- [ ] Account for container memory limits, `-XX:MaxRAMPercentage` and cgroup limits

## Concurrency

- [ ] Explain Java Memory Model happens-before rules, visibility, atomicity, and instruction reordering
- [ ] Contrast `volatile`, `synchronized`, `AtomicInteger` (CAS), and `LongAdder` cell striping
- [ ] Evaluate `ReentrantLock` features (tryLock, fairness, conditions) vs `StampedLock` optimistic reads
- [ ] Compare coordination primitives (`CountDownLatch`, `CyclicBarrier`, `Semaphore`, `Phaser`)
- [ ] Configure `ThreadPoolExecutor` parameters, bounded queues, and rejection policies (`CallerRunsPolicy`)
- [ ] Compose non-blocking pipelines with `CompletableFuture` (`thenCompose`, `thenCombine`, `allOf`, `orTimeout`)
- [ ] Diagnose Coffman deadlock conditions and apply canonical resource ordering
- [ ] Explain Java 21 Virtual Threads, carrier thread scheduling, and pinning on `synchronized` / native JNI
- [ ] Contrast `ThreadLocal` memory leaks with `ScopedValue` lexical lifetimes

## Spring Core

- [ ] Explain IoC Container and ApplicationContext vs BeanFactory hierarchy
- [ ] Trace the complete Bean Lifecycle (instantiation, BeanPostProcessor, init, destruction)
- [ ] Contrast Bean Scopes (Singleton, Prototype, Request, Session) and resolve prototype injection in singleton via `ObjectProvider`
- [ ] Compare dependency injection strategies (Constructor, Setter, Field) and diagnose circular dependency failures
- [ ] Trace Spring AOP dynamic proxy mechanisms (JDK dynamic proxies vs CGLIB subclassing) and resolve self-invocation bypass
- [ ] Configure `@Configuration` full mode (CGLIB method proxying) vs lite mode (`@Component` / records)
- [ ] Implement asynchronous event-driven decoupling with `@EventListener` and `@Async` ApplicationEventMulticaster
- [ ] Troubleshoot startup performance bottlenecks, heavy work in constructors, and deferred initialization via `ApplicationReadyEvent`
- [ ] Maintain thread safety in singleton beans with stateless designs, immutability, and thread-safe delegates

## Spring Boot

- [ ] Explain auto-configuration discovery via `AutoConfiguration.imports` and `@AutoConfiguration`
- [ ] Evaluate conditional annotations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`)
- [ ] Implement type-safe `@ConfigurationProperties` records with Jakarta Bean Validation and relaxed binding
- [ ] Trace the 17-level property resolution precedence hierarchy and multi-profile YAML activation
- [ ] Secure Actuator management endpoints and isolate Kubernetes `liveness` and `readiness` health groups
- [ ] Configure `server.shutdown: graceful` and `ThreadPoolTaskExecutor` shutdown hooks for zero-downtime rolling deploys
- [ ] Enable Java 21 Virtual Threads via `spring.threads.virtual.enabled=true` for embedded Tomcat workers
- [ ] Utilize Testcontainers `@ServiceConnection` and Docker Compose integration for automated local development

## Spring MVC

- [ ] Trace the `DispatcherServlet` request dispatching loop (`HandlerMapping`, `HandlerAdapter`, `HandlerInterceptor`)
- [ ] Contrast `@Controller` vs `@RestController` and configure `HttpMessageConverter` pipelines
- [ ] Enforce request validation using Jakarta `@Valid` on records and handle `MethodArgumentNotValidException`
- [ ] Return semantic HTTP status codes (`201 Created` with `Location`, `204 No Content`, `400 Bad Request`, `404 Not Found`)
- [ ] Centralize error handling with `@RestControllerAdvice` and RFC 9457 `ProblemDetail` without leaking internal traces
- [ ] Keep controllers thin by delegating domain logic, pricing, and transactions to domain services
- [ ] Offload long-running operations asynchronously using `DeferredResult` and `CompletableFuture` on isolated thread pools
- [ ] Harden CORS policies with explicit origin whitelists; prevent wildcard origin with credentials vulnerabilities
- [ ] Build custom `HandlerMethodArgumentResolver` and `ResponseBodyAdvice` extensions

## Spring Transactions

- [ ] Trace `@Transactional` AOP proxy mechanics and resolve self-invocation proxy bypasses
- [ ] Contrast `PlatformTransactionManager`, `TransactionDefinition`, and `TransactionStatus`
- [ ] Evaluate Propagation levels (`REQUIRED`, `REQUIRES_NEW`, `NESTED`, `MANDATORY`, `SUPPORTS`)
- [ ] Prevent HikariCP pool exhaustion deadlocks caused by `REQUIRES_NEW` or remote network I/O
- [ ] Diagnose `UnexpectedRollbackException` in shared multi-service transaction propagation
- [ ] Configure explicit `rollbackFor = Exception.class` for checked exceptions vs unchecked defaults
- [ ] Optimize read-only queries with `@Transactional(readOnly = true)` to disable dirty checking
- [ ] Coordinate transactional side effects using `@TransactionalEventListener(phase = AFTER_COMMIT)`
- [ ] Implement Transactional Outbox pattern for atomic database persistence and message publishing

## JPA / Hibernate

- [ ] Explain the 4 Entity Lifecycle States (Transient, Managed, Detached, Removed) and transition triggers
- [ ] Contrast primary key generation strategies (`IDENTITY` vs `SEQUENCE` with `allocationSize`) and JDBC batching
- [ ] Override JPA default `FetchType.EAGER` on `@ManyToOne` and `@OneToOne` with `FetchType.LAZY`
- [ ] Eliminate N+1 query storms using `JOIN FETCH`, `@EntityGraph`, or DTO projections
- [ ] Implement proxy-safe `equals()` and `hashCode()` using natural business keys without relying on generated IDs
- [ ] Prevent `LazyInitializationException` without using Open Session in View (OSIV)
- [ ] Model many-to-many associations safely without `CascadeType.REMOVE` on shared entities
- [ ] Protect presentation boundaries with Java record DTO projections instead of exposing entities
- [ ] Evaluate Optimistic Locking (`@Version`) vs Pessimistic Locking (`PESSIMISTIC_WRITE`) for concurrent writes
- [ ] Process large datasets with Hibernate `StatelessSession` or periodic `em.flush()` / `em.clear()`

## Database / SQL

- [ ] Explain 1NF, 2NF, 3NF, BCNF and trade-offs of intentional denormalization
- [ ] Apply the Leftmost Prefix Rule and Equality-Then-Range principle to composite B-Tree indexes
- [ ] Utilize Covering Indexes (`INCLUDE`) for Index-Only Scans and Partial Indexes for filtered subsets
- [ ] Interpret `EXPLAIN (ANALYZE, BUFFERS)` execution plans, shared buffer hits, and I/O reads
- [ ] Contrast physical SQL join algorithms (Nested Loop, Hash Join, Merge Join)
- [ ] Differentiate ANSI SQL isolation levels and identify Dirty Reads, Non-Repeatable Reads, Phantoms, and Write Skew
- [ ] Explain PostgreSQL MVCC mechanics (`xmin`/`xmax`), dead tuples, and Autovacuum maintenance
- [ ] Eliminate lost updates via atomic database updates (`UPDATE ... WHERE balance >= :amount`) or row locks
- [ ] Implement Keyset / Seek pagination to eliminate linear $O(N)$ `OFFSET` query degradation and pagination drift
- [ ] Design lock-free worker queues using `SELECT ... FOR UPDATE SKIP LOCKED`
- [ ] Prevent schema migration downtime with the 4-phase Expand and Contract (Parallel Run) pattern
- [ ] Size HikariCP connection pools using the $((\text{CPU Cores} \times 2) + 1)$ formula

## REST API

- [ ] Adhere to HTTP safe (`GET`, `HEAD`) and idempotent (`PUT`, `DELETE`, `GET`) protocol method semantics
- [ ] Return standard HTTP status codes (`201 Created` with `Location`, `204 No Content`, `404 Not Found`, `422 Unprocessable Entity`)
- [ ] Mandate `Idempotency-Key` headers on mutating POST endpoints to prevent duplicate operations during network retry storms
- [ ] Standardize all error responses on RFC 9457 `ProblemDetail` via `@RestControllerAdvice`
- [ ] Prevent Mass Assignment (CWE-915) and entity leakage by decoupling persistence models with explicit DTO records
- [ ] Enforce strict pagination bounds (`@Max(100)`) and return envelope metadata with next/prev cursors
- [ ] Prevent Lost Update race conditions on concurrent mutations using `ETag` and `If-Match` headers

## Spring Security

- [ ] Explain the Servlet Filter architecture: `DelegatingFilterProxy` and `FilterChainProxy` (`SecurityFilterChain`)
- [ ] Configure `authorizeHttpRequests` with strict top-down ordering from most-specific to least-specific
- [ ] Implement robust password hashing with `BCryptPasswordEncoder(12)` or `Argon2PasswordEncoder` using constant-time verification
- [ ] Prevent Insecure Direct Object References (IDOR / BOLA) by validating resource ownership against authenticated `Principal`
- [ ] Apply defense-in-depth method security using `@EnableMethodSecurity` and SpEL `@PreAuthorize`
- [ ] Implement stateless cryptographic JWT validation with signature verification, algorithm whitelisting, and expiration checks
- [ ] Distinguish CSRF protection requirements for cookie-based stateful sessions vs purely stateless Bearer token APIs
- [ ] Whitelist explicit CORS origins; prevent wildcard `*` with `allowCredentials(true)`
- [ ] Sanitize security audit logs to prevent `Authorization` header, cookie, and password exposure
- [ ] Manage `SecurityContextHolder` lifecycle across asynchronous and Virtual Thread boundaries

## Testing

- [ ] Assert observable behaviour at the service boundary instead of mocking value collaborators or verifying internal call order
- [ ] Replace boundary mocks with real contract checks so HTTP path, payload and serialization drift cannot pass unnoticed
- [ ] Build per-test immutable fixtures with test data builders instead of shared mutable state
- [ ] Await asynchronous outcomes with Awaitility bounded timeouts instead of `Thread.sleep`
- [ ] Test persistence against real PostgreSQL via Testcontainers `@ServiceConnection` instead of H2 or in-memory substitutes that diverge from production semantics
- [ ] Keep suites order-independent and parallel-safe by isolating all state per test
- [ ] Reuse a singleton PostgreSQL container across integration tests to bound startup cost
- [ ] Verify consumer expectations with WireMock stubs and recorded request assertions
- [ ] Model test data with explicit builders that make valid, minimal and edge-case inputs obvious
- [ ] Use mutation testing (PIT) to measure assertion strength instead of chasing line coverage

## Caching / Redis

- [ ] Contrast Cache-Aside, Read-Through, Write-Through, and Write-Behind caching patterns
- [ ] Evaluate trade-offs between local in-memory (Caffeine) and distributed (Redis) caches
- [ ] Master Spring Cache annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`, `condition`, `unless`)
- [ ] Prevent Cache Stampede (Thundering Herd) on hot keys via distributed mutexes or probabilistic early refresh
- [ ] Defend against Cache Penetration via short-lived sentinel null caching and Bloom filters
- [ ] Eliminate Cache Avalanche through randomized TTL jitter and clustered topology
- [ ] Select appropriate Redis memory eviction policies (`allkeys-lru`, `volatile-lru`, `noeviction`)
- [ ] Namespace all cache keys with tenant identifiers to enforce strict multi-tenant isolation
- [ ] Implement atomic distributed locks using `SET key token NX PX` with Lua script release
- [ ] Defer cache invalidations to `afterCommit` in `@Transactional` methods to prevent dirty reads on rollback

## Kafka

- [ ] Explain append-only commit log architecture, sequential disk I/O, and OS Page Cache zero-copy reads
- [ ] Design partition keys using Murmur2 hashing to guarantee total message ordering per business entity
- [ ] Configure producer durability with `acks=all`, `min.insync.replicas=2`, and `enable.idempotence=true`
- [ ] Implement idempotent consumers using an atomic deduplication store to handle at-least-once redeliveries
- [ ] Control offset acknowledgment strictly after business processing and database commits (`AckMode.MANUAL_IMMEDIATE`)
- [ ] Isolate deterministic poison pill messages immediately to Dead Letter Topics (`.DLT`) with bounded retries
- [ ] Prevent consumer group rebalance storms by offloading heavy processing and respecting `max.poll.interval.ms`
- [ ] Eliminate stop-the-world partition revocations using `CooperativeStickyAssignor`
- [ ] Prevent dual-write race conditions between databases and Kafka using the Transactional Outbox pattern or post-commit events
## RabbitMQ

- [ ] Differentiate Direct, Topic, Fanout, and Headers AMQP exchange routing semantics
- [ ] Ensure at-least-once processing via manual acknowledgments (`basicAck`) issued strictly after downstream mutations complete
- [ ] Eliminate poison pill crash loops by rejecting deterministic errors (`basicReject(requeue = false)`) to Dead Letter Exchanges
- [ ] Configure DLX and parking lot queues via `x-dead-letter-exchange` and `x-dead-letter-routing-key` queue arguments
- [ ] Optimize consumer throughput and JVM memory using bounded QoS prefetch (`basicQos(prefetchCount)`)
- [ ] Guarantee producer message delivery using Correlated Publisher Confirms and mandatory unroutable returns handling
- [ ] Implement atomic message deduplication on consumer command IDs to guarantee idempotency across network redeliveries
- [ ] Contrast Quorum Queues (Raft consensus) with Classic Mirrored Queues for high availability and network partition resilience
- [ ] Handle broker memory and disk alarms (`vm_memory_high_watermark`, `disk_free_limit`) by sizing queues and provisioning monitoring alerts

## AWS Messaging

- [ ] Contrast Amazon SQS Standard vs SQS FIFO regarding ordering guarantees, deduplication, and throughput
- [ ] Size SQS `VisibilityTimeout` relative to consumer execution latency and implement dynamic lease heartbeating
- [ ] Configure SQS Long Polling (`WaitTimeSeconds = 20`) to eliminate empty receive responses and reduce API costs
- [ ] Design poison pill mitigation using SQS Dead Letter Queues (DLQ) and `maxReceiveCount` redrive policies
- [ ] Partition SQS FIFO throughput across high-cardinality `MessageGroupId` keys up to 70,000 messages per second
- [ ] Leverage SNS-to-SQS fanout patterns with Subscription Filter Policies to eliminate noisy consumer traffic
- [ ] Route schema-aware enterprise events using Amazon EventBridge custom event buses and JSON pattern matching
- [ ] Evaluate architectural trade-offs across Kafka, RabbitMQ, SQS, SNS, and EventBridge for microservice workloads
- [ ] Scale consumer worker fleets based on backlog per worker instance (`ApproximateNumberOfMessagesVisible`) rather than CPU

## Distributed Systems

- [ ] Identify and remediate the 8 fallacies of distributed computing in network microservice designs
- [ ] Evaluate CAP and PACELC trade-offs to select appropriate consistency models (linearizability vs eventual)
- [ ] Calculate replica quorum overlap using $R + W > N$ to guarantee fresh read state
- [ ] Prevent silent data loss and lost updates caused by physical wall-clock drift and NTP skew
- [ ] Sequence distributed events causally using Lamport Timestamps, Vector Clocks, and Hybrid Logical Clocks (HLC)
- [ ] Eliminate dual-write split-brain hazards in distributed locking using monotonic fencing tokens
- [ ] Contrast quorum consensus protocols (Raft, Paxos) with blocking Two-Phase Commit (2PC)
- [ ] Protect services against cascading failure using load shedding, backpressure, and jittered retries

## Distributed Data Patterns

- [ ] Identify the blocking coordinator problem and CAP availability penalties that make Two-Phase Commit (2PC) unsuitable for microservices
- [ ] Eliminate the Dual Write problem by implementing the Transactional Outbox pattern
- [ ] Commit domain entity state changes and outbox event insertions in the exact same local database transaction
- [ ] Contrast Polling Outbox (`SELECT FOR UPDATE SKIP LOCKED`) with Change Data Capture (Debezium WAL streaming)
- [ ] Guarantee idempotent consumer execution by implementing the Inbox Pattern backed by unique constraints (`message_id`, `consumer_group`)
- [ ] Recognize that true end-to-end exactly-once delivery is achieved via At-Least-Once Delivery + Idempotent Processing
- [ ] Contrast Saga Orchestration (central state machine) with Saga Choreography (event-driven reactions)
- [ ] Guarantee that all Saga compensating transactions are strictly idempotent to prevent catastrophic double refunds on message redelivery
- [ ] Identify the Pivot Transaction in a distributed Saga, separating compensatable steps from forward-retryable steps
- [ ] Design outbox tables with date/time range partitioning to eliminate PostgreSQL MVCC dead-tuple table bloat

## Resilience

- [ ] Establish explicit multi-layer timeouts: Connect Timeout ($\le 1\text{s}$), Socket Read Timeout ($\le 3\text{s}$), and End-to-End Execution Timeout
- [ ] Bound retries ($\le 3$ attempts) and apply Exponential Backoff with randomized Full Jitter to prevent synchronized thundering herds
- [ ] Enforce client-generated `Idempotency-Key` headers on mutating requests retried across ambiguous socket read timeouts
- [ ] Classify transient network failures (503, 504) versus permanent poison pills (400, 401, 402) to fail fast immediately
- [ ] Configure Circuit Breaker state transitions (`CLOSED` $\to$ `OPEN` $\to$ `HALF_OPEN`) with failure rate and slow call rate thresholds
- [ ] Select appropriate sliding window types: Count-Based for high throughput, Time-Based for low/bursty traffic
- [ ] Isolate critical downstream dependencies using Semaphore Bulkheads (ideal for Java 21 Virtual Threads) or ThreadPool Bulkheads
- [ ] Protect internal server capacity during saturation events using dynamic Load Shedding based on Little's Law and RTT gradients
- [ ] Understand Resilience4j Spring AOP aspect evaluation order: $\text{Retry} \to \text{CircuitBreaker} \to \text{RateLimiter} \to \text{TimeLimiter} \to \text{Bulkhead}$
- [ ] Distinguish Fail-Closed security authorization requirements from Fail-Open UI and recommendation graceful degradation

## Spring Cloud

- [ ] Prevent Netty event loop starvation in Spring Cloud Gateway by eliminating all blocking calls (`block()`, `get()`, synchronous JDBC)
- [ ] Configure multi-layer gateway timeouts (global connect/response timeouts in Netty client + per-route metadata overrides)
- [ ] Implement Redis-backed token bucket rate limiting (`RequestRateLimiter`) with custom `KeyResolver` on edge routes
- [ ] Sanitize external requests by stripping sensitive internal routing headers (`X-User-Id`, `X-Internal-Secret`) via `RemoveRequestHeader`
- [ ] Enforce explicit `Request.Options` timeouts (connect $\le 500\text{ms}$, read $\le 2000\text{ms}$) on all OpenFeign clients
- [ ] Implement custom Feign `ErrorDecoder` to translate 4xx client errors into domain exceptions and mark 503/504 as `RetryableException`
- [ ] Eliminate blind retries (`Retryer.NEVER_RETRY`) in Feign to prevent retry storms and duplicate mutations
- [ ] Understand `@RefreshScope` CGLIB proxy eviction mechanics and evaluate thundering herd hazards during dynamic config reloads
- [ ] Contrast Spring Cloud components with modern Kubernetes and AWS infrastructure (CoreDNS vs Eureka, K8s Service vs Ribbon, ConfigMaps vs Config Server)
- [ ] Evaluate migration paths from OpenFeign to Spring 6 native `HttpInterfaces` and `RestClient`

## WebClient / WebFlux

- [ ] Explain event loop architecture vs thread-per-request, epoll/kqueue socket multiplexing, and the cardinal rule of non-blocking
- [ ] Understand the 4 core Reactive Streams interfaces (`Publisher`, `Subscriber`, `Subscription`, `Processor`) and the `request(n)` contract
- [ ] Contrast `Mono<T>` (0..1) vs `Flux<T>` (0..N) and explain the three pipeline phases (Assembly, Subscription, Execution)
- [ ] Differentiate `subscribeOn(Scheduler)` (upstream source thread) from `publishOn(Scheduler)` (downstream operator thread)
- [ ] Implement backpressure overflow strategies (`onBackpressureBuffer`, `onBackpressureDrop`, `onBackpressureLatest`)
- [ ] Configure multi-layer WebClient timeouts (Netty TCP connect timeout, response timeout, and stream-level `.timeout(Duration)`)
- [ ] Bound `Flux.flatMap(fn, maxConcurrency)` concurrency to prevent connection pool exhaustion and `PoolAcquireTimeoutException`
- [ ] Isolate errors in multi-publisher composition (`Mono.zip`) using `.onErrorReturn()` and `.onErrorResume()`
- [ ] Understand Reactor `Schedulers` (`parallel()`, `boundedElastic()`, `single()`) and safely offload blocking I/O (JDBC, legacy clients)
- [ ] Diagnose and prevent event loop blocking using BlockHound bytecode instrumentation in CI/CD pipelines
- [ ] Compare R2DBC vs JDBC and evaluate trade-offs (loss of JPA/Hibernate, reactive transactions via Reactor Context)
- [ ] Evaluate Spring WebFlux vs Spring MVC with Java 21 Virtual Threads (when to use each, when WebFlux is an anti-pattern)

## Observability

- [ ] Explain the three pillars of observability (logs, metrics, traces), their data models, storage costs, and diagnostic trade-offs
- [ ] Implement structured JSON logging using Logstash encoder and prevent log injection risks
- [ ] Propagate SLF4J Mapped Diagnostic Context (MDC) across asynchronous thread pools using `TaskDecorator` and prevent thread context leaks
- [ ] Understand Micrometer core meter types (`Counter`, `Timer`, `Gauge`, `DistributionSummary`) and know when to use each
- [ ] Prevent metric cardinality explosion in Prometheus/Micrometer by restricting dimensions to low-cardinality enums ($< 100$ combinations)
- [ ] Explain why arithmetic mean latency obscures tail latency and configure Micrometer `Timer` with percentiles (p50, p95, p99) and SLO buckets
- [ ] Understand the W3C TraceContext standard (`traceparent: 00-{traceId}-{spanId}-{flags}`) and trace propagation across HTTP and Kafka headers
- [ ] Apply the Micrometer Observation API to unify metrics and distributed tracing under a single lifecycle
- [ ] Differentiate the RED method (Rate, Errors, Duration) for services from the USE method (Utilization, Saturation, Errors) for resources
- [ ] Enforce data privacy and PII masking rules in Logback (PCI-DSS PAN masking, omitting CVVs and passwords)
- [ ] Architect multi-window multi-burn-rate alerting strategies based on Google SRE Error Budget consumption
- [ ] Diagnose and troubleshoot thread pool starvation and deadlocks using ThreadMXBean and Actuator thread dumps

## Performance

- [ ] Distinguish latency, throughput, utilization, saturation, and queue time
- [ ] Explain p95/p99 and why averages hide tails
- [ ] Apply Little's Law to a capacity estimate and state its assumptions
- [ ] Budget Hikari connections across maximum application replicas
- [ ] Size CPU and blocking executors from workload evidence
- [ ] Design bounded queues, rejection, and load shedding
- [ ] Diagnose N+1 queries with statement counts and traces
- [ ] Use JFR to find CPU, allocation, GC, and lock bottlenecks
- [ ] Explain coordinated omission and open versus closed load models
- [ ] Design a reproducible JMH or end-to-end load experiment
- [ ] Diagnose Hikari exhaustion with low database CPU
- [ ] Explain when lower-level allocation optimization is justified

## Docker

- [ ] Explain Linux container primitives (namespaces for isolation, cgroups for resource metering, overlayfs for copy-on-write layering)
- [ ] Architect multi-stage Docker builds separating build environment (JDK, Gradle, source) from minimal production runtime (JRE, distroless)
- [ ] Implement Spring Boot layered JAR extraction (`layertools`) and order Dockerfile `COPY` instructions by cache change frequency
- [ ] Configure non-root dedicated application users (`useradd -u 10001`) and explain Linux file permission / UID mapping semantics
- [ ] Explain cgroups v1 vs v2 mechanics and why legacy JVMs ignored container memory limits, leading to host-level OOMKills
- [ ] Size JVM container memory correctly: allocate $\sim 70-75\%$ to heap via `-XX:MaxRAMPercentage` and reserve $25-30\%$ for non-heap overhead
- [ ] Contrast `ENTRYPOINT` exec form (`["java", "-jar", "..."]`) vs shell form, explaining PID 1 signal forwarding and SIGTERM handling
- [ ] Implement two-phase graceful shutdown with Spring Boot (`server.shutdown=graceful`) and orchestrator termination grace periods
- [ ] Configure Docker and Kubernetes health probes (distinguishing liveness vs readiness probes and avoiding circular dependencies)
- [ ] Prevent secrets exposure in Docker images: never use `ARG` or `ENV` for credentials; use BuildKit `--mount=type=secret` or runtime injection
- [ ] Compare container base image distributions (Alpine vs Debian-slim vs Distroless vs Chainguard) and evaluate musl vs glibc trade-offs

## AWS

- [ ] Explain operational tradeoffs between compute models: EC2 (IaaS), ECS Fargate (managed containers), EKS (Kubernetes), and AWS Lambda (Serverless)
- [ ] Understand AWS Lambda SnapStart for Java (CRaC memory checkpointing, $< 200\text{ms}$ cold starts) and avoid snapshot state replay bugs (re-seeding `SecureRandom`)
- [ ] Differentiate IAM Identity-Based Policies vs Resource-Based Policies and evaluate cross-account evaluation logic
- [ ] Enforce Principle of Least Privilege: eliminate wildcard `"Action": "*"` and `"Resource": "*"` across S3, SQS, and KMS
- [ ] Distinguish ECS Task Role (runtime application domain permissions) from ECS Task Execution Role (agent infrastructure bootstrap)
- [ ] Implement EKS IAM Roles for Service Accounts (IRSA) with OIDC federation to prevent container-to-node privilege escalation
- [ ] Externalize application credentials using AWS Secrets Manager (automated rotation) and SSM Parameter Store (SecureString) with KMS CMKs
- [ ] Implement Envelope Encryption with AWS KMS (`GenerateDataKey` with 256-bit DEK, local AES-GCM encryption, immediate plaintext memory zeroing)
- [ ] Compare Amazon RDS Multi-AZ ($RPO = 0$, automated standby failover in $< 120\text{s}$) with Read Replicas (asynchronous replication lag)
- [ ] Explain Amazon Aurora distributed storage architecture: 6-way replication across 3 AZs, 4/6 write quorum, and instantaneous crash recovery
- [ ] Prevent database connection exhaustion using Amazon RDS Proxy to multiplex connections across autoscaled ECS tasks and Lambda bursts
- [ ] Differentiate VPC Gateway Endpoints (free for S3/DynamoDB) from Interface Endpoints (PrivateLink) to prevent NAT Gateway data transfer costs
- [ ] Synchronize ALB Target Group Deregistration Delay with Spring Boot Graceful Shutdown to eliminate HTTP 502 Bad Gateway errors during deployments

## CI/CD

- [ ] Explain pipeline stages (compile, verify, static analysis, unit/integration test, container build, CVE scan, signing, deploy)
- [ ] Tag container images strictly with immutable Git commit SHAs; eliminate mutable `:latest` tagging in production
- [ ] Understand the Dual-State Coexistence Invariant: ensure database and API changes are backward-compatible with running $v1$ code
- [ ] Execute database refactoring via the Expand-Contract (Parallel Change) pattern across multiple releases (never drop columns in release DDL)
- [ ] Set strict `lock_timeout` ($\le 3\text{s}$) in PostgreSQL DDL migrations to prevent connection pool exhaustion cascades
- [ ] Use `CREATE INDEX CONCURRENTLY` in PostgreSQL to build production indexes without blocking writes
- [ ] Disable in-app migrations in production (`spring.flyway.enabled=false`); execute Flyway as a dedicated pre-deployment pipeline job
- [ ] Compare deployment strategies: Rolling Updates (low cost, $v1/v2$ coexist), Blue/Green (instant cutover/rollback, 2x cost), Canary (blast radius containment)
- [ ] Configure automated deployment health gates (`/actuator/health/readiness`), synthetic smoke tests, and automated rollback triggers
- [ ] Scan container images and dependencies with Trivy/Grype, enforcing zero unpatched `CRITICAL` or `HIGH` CVEs
- [ ] Cryptographically sign release artifacts using Sigstore Cosign with keyless OIDC authentication
- [ ] Implement architectural fitness functions in Java using ArchUnit to enforce package boundaries and prevent code rot
- [ ] Decouple deployment from release using Feature Flags (Strategy pattern, operational kill switches, and technical debt retirement)
- [ ] Contrast push-based CI/CD pipelines with declarative pull-based GitOps (ArgoCD reconciliation loops, drift detection, sync waves)

## System Design

- [ ] Frame high-scale system design using a structured 4-step framework: Scope Requirements, High-Level Design, Deep-Dive Critical Components, and Scale/Bottlenecks
- [ ] Formulate Back-of-the-Envelope capacity estimates: QPS (Read/Write), network bandwidth ingress/egress, and 5-year storage growth with indexing overhead
- [ ] Evaluate the CAP Theorem and PACELC trade-offs across storage engines (CP vs AP under network partitions; Latency vs Consistency in normal operations)
- [ ] Choose appropriate database paradigms: Relational (PostgreSQL, ACID, complex joins), Document (MongoDB, flexible schema), Key-Value (Redis, sub-millisecond cache), Columnar (Cassandra/ClickHouse, append-heavy time-series)
- [ ] Apply distributed caching topologies (Cache-Aside, Write-Through, Write-Behind, Refresh-Ahead) and prevent Cache Stampede, Cache Penetration, and Cache Avalanche
- [ ] Design distributed rate limiters using Token Bucket or Sliding Window Counter with atomic Redis Lua scripts to eliminate multi-round-trip latency and window boundary bursts
- [ ] Implement robust horizontal database scaling: Read Replicas with replication lag management, Range vs Hash Partitioning, and Consistent Hashing with virtual nodes
- [ ] Decouple microservice boundaries using Event-Driven Architecture, Transactional Outbox, and Idempotent Consumers over brittle distributed transactions (2PC/XA)
- [ ] Guarantee financial consistency in payment workflows via client-generated Idempotency Keys (UUIDv7) propagated through all gateway calls
- [ ] Architect high-concurrency flash sale inventory reservations using Redis in-memory atomic decrement (`DECRBY`) and asynchronous order queues to protect relational databases
- [ ] Compare distributed ID generation schemes: Snowflake (64-bit time-sortable), UUIDv7, and Centralized Range Allocation (ZooKeeper/Redis ticket service)
- [ ] Select appropriate network communication protocols: REST/JSON (public APIs), gRPC/HTTP/2 (internal microservice RPC), WebSockets/SSE (real-time bi-directional feeds)

## Architecture

- [ ] Contrast traditional Layered Architecture (database-driven) with Hexagonal (Ports & Adapters) and Clean Architecture (Dependency Rule)
- [ ] Design pure domain models with zero framework imports (`jakarta.persistence.*`, `org.springframework.*`) to ensure microsecond unit testability
- [ ] Differentiate Driving (Inbound / Use Cases) from Driven (Outbound / Persistence, Gateways) Ports and their respective adapters
- [ ] Identify and refactor Anemic Domain Models (procedural property bags with public setters) into Rich Domain Aggregate Roots
- [ ] Distinguish Entities (persistent lifecycle identity) from Value Objects (immutable, attribute-based equality, self-validating)
- [ ] Enforce Aggregate consistency boundaries: modify exactly one aggregate root per database transaction and coordinate via Domain Events
- [ ] Separate Domain Services (pure domain calculation spanning multiple models, zero I/O) from Application Services (use case orchestration)
- [ ] Architect Modular Monoliths using package-private visibility, public API interfaces, and Spring Modulith event-driven inter-module communication
- [ ] Codify architectural fitness functions in Java using ArchUnit to automatically fail CI builds on boundary or layer violations
- [ ] Apply CQRS (Command Query Responsibility Segregation) to decouple write-optimized aggregates from read-optimized denormalized projections
- [ ] Explain Event Sourcing mechanics: append-only event store, optimistic concurrency control via sequence numbers, and snapshotting
- [ ] Prevent the Shared Database anti-pattern across bounded contexts to protect autonomous schema evolution and team ownership
- [ ] Differentiate DDD (structural domain modeling & aggregates) from BDD (collaborative behavior discovery & executable specifications)
- [ ] Implement Outside-In development: map BDD Given-When-Then scenarios to Hexagonal Driving Ports and verify Aggregate Root state transitions
- [ ] Apply the Backend for Frontend (BFF) pattern: shape client-specific payloads (Mobile vs Web), reduce round-trips, and ban domain logic leaks into BFF adapters

## Related

- [Roadmap](roadmap.md)
- [Java questions](questions/java.md)
- [JVM & Performance questions](questions/jvm.md)
- [Concurrency questions](questions/concurrency.md)
- [Spring Core questions](questions/spring-core.md)
- [Spring Boot questions](questions/spring-boot.md)
- [Spring MVC questions](questions/spring-mvc.md)
- [Spring Transactions questions](questions/spring-transactions.md)
- [JPA / Hibernate questions](questions/jpa-hibernate.md)
- [Database / SQL questions](questions/database-sql.md)
- [REST API questions](questions/rest-api.md)
- [Spring Security questions](questions/spring-security.md)
- [Testing questions](questions/testing.md)
- [Caching / Redis questions](questions/caching-redis.md)
- [Kafka questions](questions/kafka.md)
- [RabbitMQ questions](questions/rabbitmq.md)
- [AWS Messaging questions](questions/aws-messaging.md)
- [Distributed Systems questions](questions/distributed-systems.md)
- [Distributed Data Patterns questions](questions/distributed-data-patterns.md)
- [Resilience questions](questions/resilience.md)
- [Spring Cloud questions](questions/spring-cloud.md)
- [WebClient / WebFlux questions](questions/webclient-webflux.md)
- [Observability questions](questions/observability.md)
- [Performance questions](questions/performance.md)
- [Docker questions](questions/docker.md)
- [AWS questions](questions/aws.md)
- [CI/CD questions](questions/ci-cd.md)
- [System Design questions](questions/system-design.md)
- [Architecture questions](questions/architecture.md)
