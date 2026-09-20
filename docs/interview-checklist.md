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

