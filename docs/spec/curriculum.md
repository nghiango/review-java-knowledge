# Curriculum

The 30 modules, their canonical slugs, module type, required topic coverage and required broken
examples. Structure rules are in [module-conventions.md](module-conventions.md).

## Slug registry

The slug is used verbatim for `modules/NN-<slug>`, Gradle project `:modules:NN-<slug>` and
`docs/topics/<slug>/`. The Java package root is normally `lab.<slug without dashes>`; the
**Package root column below is authoritative** and shortens two long slugs
(`distributed-data-patterns` → `lab.distributeddata`, `webclient-webflux` → `lab.webflux`).

| # | Slug | Type | Package root |
|---|---|---|---|
| 01 | core-java | code | `lab.corejava` |
| 02 | jvm | code | `lab.jvm` |
| 03 | concurrency | code | `lab.concurrency` |
| 04 | spring-core | code | `lab.springcore` |
| 05 | spring-boot | code | `lab.springboot` |
| 06 | spring-mvc | code | `lab.springmvc` |
| 07 | spring-transactions | code | `lab.springtransactions` |
| 08 | jpa-hibernate | code | `lab.jpahibernate` |
| 09 | database-sql | code | `lab.databasesql` |
| 10 | rest-api | code | `lab.restapi` |
| 11 | spring-security | code | `lab.springsecurity` |
| 12 | testing | code | `lab.testing` |
| 13 | caching-redis | code | `lab.cachingredis` |
| 14 | kafka | code | `lab.kafka` |
| 15 | rabbitmq | code | `lab.rabbitmq` |
| 16 | aws-messaging | doc (optional LocalStack code) | `lab.awsmessaging` |
| 17 | distributed-systems | doc | — |
| 18 | resilience | code | `lab.resilience` |
| 19 | distributed-data-patterns | code | `lab.distributeddata` |
| 20 | spring-cloud | doc | — |
| 21 | webclient-webflux | code | `lab.webflux` |
| 22 | observability | code | `lab.observability` |
| 23 | performance | code | `lab.performance` |
| 24 | docker | doc | — |
| 25 | aws | doc | — |
| 26 | ci-cd | doc | — |
| 27 | system-design | doc | — |
| 28 | architecture | code | `lab.architecture` |
| 29 | design-patterns | code | `lab.designpatterns` |
| 30 | senior-engineering | doc | — |

Implementation order: bootstrap → 01…12 fully → 13…30. Module 07 is the anchor example used
across all specs; prove the conventions on it early.

Numbers 01–30 are reserved for baseline topics. Track-only modules use ≥ 40
(e.g. `40-whats-new`, `40-kotlin-spring-idioms`); a track module that extends a baseline topic
reuses the baseline number and slug. Track curricula are in [tracks.md](tracks.md) §5–6.

---

## 01 — Core Java

**Cover:** OOP, encapsulation, abstraction, inheritance, polymorphism, composition vs inheritance,
SOLID; `equals`/`hashCode`, `==` vs `equals`; immutability, records, enums, sealed classes,
pattern matching (`instanceof`, `switch`, record patterns), text blocks; String internals,
StringBuilder, string pool; pass-by-value, `final`, `static`; Comparable/Comparator; exceptions,
checked vs unchecked, try-with-resources, exception chaining; Optional; collections (List, Set,
Map, Queue, Deque, ArrayList, LinkedList, HashSet, TreeSet, LinkedHashMap, HashMap, TreeMap,
ConcurrentHashMap, sequenced collections), HashMap internals (hashing, collisions,
treeification, resize), fail-fast iterators; generics, bounded types, wildcards, PECS, type
erasure; streams (laziness, stateful vs stateless ops, collectors, short-circuiting, parallel
streams); `var`; `java.time`.

**Broken examples (≥ 4):** mutable keys in HashMap · inconsistent equals/hashCode · Optional
misuse (`get()`, Optional fields/params) · stream side effects and shared mutable state ·
resource leak without try-with-resources · swallowed / over-broad exception handling ·
ConcurrentModificationException · parallel stream misuse on I/O-bound work.

## 02 — JVM

**Cover:** JVM architecture, JDK/JRE/JVM, bytecode; class loading (bootstrap/platform/application
loaders, delegation, linking, initialization order); runtime areas (heap, stack, metaspace, PC,
native stack); object allocation, TLAB, escape analysis; JIT (C1/C2, tiered), AOT/CDS/GraalVM
concepts; GC (generational hypothesis, young/old, G1, ZGC, Shenandoah, STW, GC roots, safepoints);
memory leaks; heap dumps, thread dumps, JFR, `jcmd`; OutOfMemoryError variants,
StackOverflowError; container awareness (`MaxRAMPercentage`).

**Broken examples (≥ 3):** static collection leak · ThreadLocal leak in pooled threads ·
unbounded in-memory cache · excessive short-lived allocation in hot path · classloader leak
concept (doc-only example).

## 03 — Concurrency

**Cover:** processes vs threads; race conditions, atomicity, visibility, ordering; Java Memory
Model, happens-before; `synchronized`, `volatile`, atomics, CAS, ABA; ReentrantLock,
ReadWriteLock, StampedLock, Semaphore, CountDownLatch, CyclicBarrier, Phaser; ExecutorService,
thread pool sizing, rejection policies, ForkJoinPool; CompletableFuture composition and error
handling; ConcurrentHashMap, BlockingQueue; deadlock, livelock, starvation; virtual threads,
pinning (`synchronized` in Java 21), ScopedValue vs ThreadLocal, structured concurrency
concepts; CPU-bound vs I/O-bound workloads; `spring.threads.virtual.enabled`.

**Broken examples (≥ 5):** lost update on shared counter · deadlock from inconsistent lock
ordering · `volatile` used for compound action · unbounded thread creation · CompletableFuture
without exception handling / `join()` in loop · blocking common ForkJoinPool · thread pool
starvation (pool waits on itself).

## 04 — Spring Core

**Cover:** IoC, DI, constructor vs setter vs field injection; ApplicationContext vs BeanFactory;
bean definitions, component scanning, stereotypes, `@Bean`/`@Configuration`
(`proxyBeanMethods`); bean lifecycle, BeanPostProcessor, BeanFactoryPostProcessor,
`@PostConstruct`/`@PreDestroy`, `InitializingBean`; scopes, scoped proxies; circular
dependencies; proxies (JDK vs CGLIB), AOP (pointcuts, advice, `@Aspect`), self-invocation;
`@Conditional`; events (`ApplicationEventPublisher`, `@TransactionalEventListener`); `Environment`.

**Broken examples (≥ 3):** field injection with circular dependency · self-invocation bypassing
aspect · prototype bean injected into singleton · heavy work in constructor / `@PostConstruct` ·
mutable singleton state.

## 05 — Spring Boot

**Cover:** startup flow, `SpringApplication`, auto-configuration and `@AutoConfiguration`
ordering, conditional annotations, starters; `@ConfigurationProperties`, validation, profiles,
property precedence; Actuator (health groups, probes, info, env exposure); startup diagnostics
(`debug`, condition evaluation report, startup actuator); embedded servers, graceful shutdown;
Testcontainers `@ServiceConnection` and Docker Compose support; Spring Modulith overview
(optional); virtual threads switch.

**Broken examples (≥ 3):** exposing all actuator endpoints unauthenticated · `@Value` scattered
config with no validation · profile-specific secrets in git · no graceful shutdown with in-flight
work · auto-configuration overridden accidentally.

## 06 — Spring MVC

**Cover:** DispatcherServlet flow, HandlerMapping, HandlerAdapter, argument resolvers,
`HttpMessageConverter`, Jackson configuration; validation (`@Valid`, groups); exception handling
(`@RestControllerAdvice`, `ProblemDetail`, `ResponseEntityExceptionHandler`); filters vs
interceptors; CORS; async MVC (`DeferredResult`, `Callable`), request thread model; file upload;
content negotiation; `RestClient`.

**Broken examples (≥ 4):** missing validation · leaking internal exceptions/stack traces ·
wrong HTTP status codes · god controller with business logic · blocking long work on request
thread without timeout · `allowedOrigins("*")` with credentials.

## 07 — Spring Transactions (anchor module)

**Cover:** `@Transactional` and the proxy mechanism; `PlatformTransactionManager`,
`TransactionSynchronizationManager`, connection binding; boundaries; propagation (REQUIRED,
REQUIRES_NEW, SUPPORTS, MANDATORY, NOT_SUPPORTED, NEVER, NESTED); isolation levels; rollback
rules (checked vs unchecked, `rollbackFor`, `noRollbackFor`, `UnexpectedRollbackException`);
`readOnly`, `timeout`; self-invocation, private/final methods; transactions and threads /
`@Async` / CompletableFuture; transactions and remote APIs; `TransactionTemplate`;
`@TransactionalEventListener`; connection pool lifecycle (HikariCP).

**Broken examples (≥ 6):** self-invocation · too-wide transaction · remote API inside
transaction (→ Hikari exhaustion) · incorrect rollback assumption (checked exception) · wrong
propagation (REQUIRES_NEW misuse / NESTED expectation) · hidden boundary in `@Async` · transaction
on private method · combined `order-processing-v1` PR exercise.

## 08 — JPA / Hibernate

**Cover:** JPA vs Hibernate; entity lifecycle (transient, managed, detached, removed);
persistence context, first-level cache, dirty checking, flush modes; `save` vs `persist` vs
`merge`; lazy vs eager, `LazyInitializationException`, open-in-view; N+1, fetch joins,
`@EntityGraph`, `@BatchSize`; pagination with joins; batching inserts/updates; optimistic
(`@Version`) vs pessimistic locking; relationships, cascade, orphan removal; entity equality;
auditing; projections/DTOs; second-level cache concepts.

**Broken examples (≥ 5):** N+1 · eager everything · `CascadeType.ALL` on many-to-many ·
bidirectional recursion in JSON · entity exposed through API · equals/hashCode on generated ID ·
lazy initialization outside transaction · `open-in-view` masking problems.

## 09 — Database / SQL

**Cover:** relational modelling, normalization/denormalization, keys; indexes (B-tree, composite
ordering, covering, partial, selectivity/cardinality); `EXPLAIN (ANALYZE, BUFFERS)`; joins; ACID;
isolation anomalies (dirty, non-repeatable, phantom, write skew); locks, MVCC, deadlocks;
optimistic/pessimistic concurrency, `SELECT … FOR UPDATE [SKIP LOCKED]`, advisory locks;
OFFSET vs keyset pagination; views, materialized views; partitioning; migrations (Flyway,
backward-compatible changes); connection pool sizing.

**Broken examples (≥ 5):** missing index · wrong composite index order · `SELECT *` · OFFSET
pagination on large table · lost update without locking · long transaction holding locks ·
SQL injection via string concatenation · unbounded result set · destructive migration.

## 10 — REST API

**Cover:** resources, HTTP methods, safety, idempotency; status codes; headers; caching
(`Cache-Control`, ETag); pagination, filtering, sorting; versioning; error contracts — RFC 9457
Problem Details (`ProblemDetail`); idempotency keys; backward compatibility; optimistic
concurrency with `ETag`/`If-Match`; bulk operations; long-running operations (202 + status
resource); HATEOAS concepts; OpenAPI.

**Broken examples (≥ 5):** GET mutating state · 200 for every outcome · non-idempotent retry
handling · entity leakage · inconsistent error responses · missing pagination · mass assignment ·
combined PR exercise.

## 11 — Spring Security

**Cover:** `SecurityFilterChain`, filter order; authentication vs authorization;
AuthenticationManager/Provider, `SecurityContext` and its holder strategy (incl. virtual/async
threads); sessions vs stateless; JWT (validation, claims, expiry, key rotation); OAuth2 client and
resource server, OpenID Connect; roles vs authorities; method security (`@PreAuthorize`, SpEL
pitfalls); password hashing (BCrypt/Argon2, `DelegatingPasswordEncoder`); CSRF, CORS, security
headers; input validation; rate limiting / brute-force; OWASP Top 10 mapping; secrets management.

**Broken examples (≥ 6):** plaintext / MD5 passwords · trusting user ID from payload (IDOR) ·
broken object-level authorization · overly broad `permitAll` · JWT accepted without signature/exp
validation · secrets in repo/logs · insecure CORS · logging credentials · combined PR exercise.

## 12 — Testing

**Cover:** testing pyramid; unit, integration, component, contract, E2E; JUnit 5 (parameterized,
nested, extensions), Mockito (strictness, argument captors), AssertJ; MockMvc, `@WebMvcTest`,
`@DataJpaTest`, `@SpringBootTest`, slices; Testcontainers with `@ServiceConnection`, singleton
containers; WireMock; Awaitility; database and messaging integration tests; determinism, flaky
tests; fixtures and test data builders; ArchUnit; mutation testing (PIT) concepts; consumer-driven
contracts concepts.

**Broken examples (≥ 4):** testing implementation instead of behaviour · excessive mocking hiding
integration · shared mutable fixtures · `Thread.sleep` for async · H2 hiding PostgreSQL
behaviour · test order dependency.

## 13 — Caching / Redis

**Cover:** cache-aside, read-through, write-through, write-behind; local (Caffeine) vs distributed
(Redis); Spring Cache abstraction (`@Cacheable`, `@CacheEvict`, keys, conditions); TTL,
invalidation, consistency; stampede, penetration, hot keys; distributed locks (Redisson concepts,
pitfalls); eviction policies; Redis data structures; serialization.

**Broken examples (≥ 4):** stale cache after update · stampede on expiry · caching `null` /
exceptions wrongly · missing tenant/user in key · infinite TTL · dual-write DB/cache ordering.

## 14 — Kafka

**Cover:** brokers, topics, partitions, offsets, replication, ISR, acks; producers (idempotent,
transactions, keys, batching); consumers, groups, assignment, rebalancing, lag; ordering; delivery
semantics (at-most/at-least/exactly-once concepts and myths); offset commits; DLT, poison
messages; schema evolution (Schema Registry, Avro/JSON schema). Spring Kafka: `@KafkaListener`,
`KafkaTemplate`, error handlers, retry topics, concurrency, manual ack, transactions.

**Broken examples (≥ 5):** ack before processing · non-idempotent consumer · assuming global
ordering · infinite retries · missing DLT · wrong partition key · blocking listener beyond
`max.poll.interval.ms` · combined PR exercise.

## 15 — RabbitMQ

**Cover:** broker, exchanges (direct, topic, fanout, headers), queues, bindings, routing keys;
ack/nack/reject/requeue; publisher confirms; durability, persistence; TTL, DLX/DLQ; prefetch;
competing consumers; poison messages; retry patterns (delayed exchange). Spring AMQP:
`RabbitTemplate`, `@RabbitListener`, listener containers, confirms.

**Broken examples (≥ 4):** auto-ack before processing · infinite requeue · no DLQ · huge
prefetch · non-idempotent consumer · fire-and-forget publish where delivery matters.

## 16 — AWS Messaging (doc, optional LocalStack code)

**Cover:** SQS standard vs FIFO, visibility timeout, long polling, retention, DLQ, deduplication,
message groups; SNS pub/sub, fanout, filters; EventBridge bus, rules, scheduling. Comparison
matrix: Kafka vs RabbitMQ vs SQS vs SNS vs EventBridge — when each fits.

**Broken examples (≥ 2):** visibility timeout shorter than processing time · FIFO without
deduplication id · no DLQ with redrive policy · SNS fanout without filter causing noisy consumers.

## 17 — Distributed Systems (doc)

**Cover:** fallacies of distributed computing; partial failure; CAP/PACELC; consistency models;
replication, quorums; clocks, ordering; idempotency; distributed locking pitfalls; service
discovery; load balancing; sync vs async communication; backpressure.

**Broken examples (≥ 2):** design docs with hidden assumptions (synchronous clock, exactly-once,
single-writer) to review; a "distributed lock with Redis SET NX" design without fencing tokens.

## 18 — Resilience

**Cover:** timeouts (connect/read/total); retries, amplification, exponential backoff, jitter;
circuit breaker states; bulkhead; rate limiting; load shedding; fallback; graceful degradation;
poison messages; overload protection. Resilience4j with Spring Boot, order of decorators.

**Broken examples (≥ 4):** infinite retry · retry without timeout · retrying non-idempotent call ·
retry storm across layers · missing jitter · catch-and-ignore failures.

## 19 — Distributed Data Patterns

**Cover:** distributed transactions, 2PC; Saga (orchestration, choreography, compensation);
transactional outbox, CDC (Debezium concepts); inbox pattern; idempotent consumer, duplicate
detection; eventual consistency; exactly-once myths. Build one realistic order–payment example
end-to-end (outbox + idempotent consumer, PostgreSQL + Kafka via Testcontainers).

**Broken examples (≥ 3):** dual write DB + broker · compensation that is not idempotent · inbox
without dedup key · combined PR exercise.

## 20 — Spring Cloud (doc)

**Cover conceptually:** Gateway, OpenFeign, config server, discovery, load balancing, resilience
integration, tracing. When Kubernetes/AWS replaces Spring Cloud responsibilities.

**Broken examples (≥ 2):** gateway config without timeouts/retries limits · Feign client without
timeout or error decoder.

## 21 — WebClient / WebFlux

**Cover:** blocking vs non-blocking, event loop; Reactive Streams; Mono/Flux; backpressure;
WebClient (timeouts, connection pool, retries); Reactor threading and schedulers; blocking calls in
reactive pipelines (BlockHound); R2DBC concepts; when not to use WebFlux.

**Broken examples (≥ 4):** `.block()` in request flow · blocking JDBC in WebFlux · uncontrolled
`flatMap` concurrency · missing timeout · chain without error handling.

## 22 — Observability

**Cover:** logs, metrics, traces; structured logging, MDC, correlation IDs; Micrometer Observation
API, Tracing, OTel bridge; trace propagation across HTTP/Kafka; Actuator; RED/USE; dashboards,
alerting, SLOs.

**Broken examples (≥ 4):** logging secrets/PII · missing correlation ID across threads ·
high-cardinality metric tags · swallowed exceptions · no latency histogram.

## 23 — Performance

**Cover:** latency vs throughput, percentiles; saturation; CPU/memory; HikariCP sizing; thread
pool sizing; GC impact; profiling (JFR, async-profiler); load testing (k6/Gatling concepts); JMH.

**Broken examples (≥ 4), each paired with a reproducible load scenario:** Hikari exhaustion ·
oversized/undersized thread pool · DB bottleneck · excessive allocation · N+1 · lock contention.

## 24 — Docker (doc)

**Cover:** images, layers, multi-stage builds, JVM in containers (memory/CPU limits, ergonomics),
configuration and secrets, health checks, graceful shutdown (SIGTERM), non-root, image scanning,
Spring Boot layered jars / buildpacks.

**Broken examples (≥ 2):** fat single-stage image running as root · no memory limits awareness ·
secrets baked into image · missing health check / shutdown handling.

## 25 — AWS (doc)

**Cover:** Lambda, ECS, EKS, EC2, RDS, S3, CloudWatch, IAM, Secrets Manager, KMS, SQS, SNS,
EventBridge, ALB/NLB, AZs, scaling. Backend-engineer interview depth, not certification trivia.

**Broken examples (≥ 2):** over-permissive IAM policy · secrets in environment variables /
plaintext · single-AZ RDS for critical service.

## 26 — CI/CD (doc)

**Cover:** pipeline stages (compile, test, static analysis, security scanning, artifact, image),
migrations in pipelines, deployment strategies (rolling, blue/green, canary), rollback, feature
flags, backward-compatible DB changes, zero-downtime.

**Broken examples (≥ 2):** pipeline running destructive migration before deploy · no rollback
path · tests skipped on main.

## 27 — System Design (doc)

**Exercises:** URL shortener, payment system, order system, notification system, inventory
system, distributed rate limiter, file processing system, booking system.

Each covers: requirements, NFRs, API, domain model, database, indexes, caching, messaging,
consistency, concurrency, scaling, failure handling, security, observability, deployment,
trade-offs.

**Broken examples (≥ 2):** flawed design documents to critique.

## 28 — Architecture

**Cover:** layered, clean, hexagonal, onion; modular monolith (Spring Modulith, ArchUnit rules);
microservices; DDD (bounded contexts, aggregates, repositories, domain/application services);
CQRS; event-driven; event sourcing concepts. Always trade-offs, never dogma.

**Broken examples (≥ 3):** domain depending on infrastructure · anaemic domain with god service ·
cross-module database access · ArchUnit test that catches them.

## 29 — Design Patterns

**Cover** through real Spring/backend examples: Strategy, Factory, Builder, Adapter, Decorator,
Proxy, Observer, Template Method, Chain of Responsibility, Repository, Specification, State.
Each: problem → naive approach → pattern → implementation → trade-offs → when not to use.

**Broken examples (≥ 3):** switch-on-type growth · pattern over-engineering · decorator order bug.

## 30 — Senior Engineering (doc)

**Cover:** code review practice, mentoring, architecture decisions and ADRs, technical debt,
incidents and post-mortems, production debugging, disagreements, estimation, ownership,
stakeholder communication, handling junior mistakes, delivery vs quality trade-offs.

**Broken examples (≥ 2):** poorly written ADR · blame-oriented post-mortem · unhelpful code
review comments — to rewrite.

---

## Final goal

After completing this repository a developer can: answer senior Java/Spring interview questions;
explain internals; review problematic backend code; diagnose production issues; reason about
concurrency and transaction boundaries; work effectively with SQL and Hibernate; design reliable
REST APIs; implement authentication and authorization safely; design Kafka/RabbitMQ consumers;
handle retries and duplicates; reason about distributed consistency; use caching safely; find
performance bottlenecks; design resilient, observable services; discuss architecture trade-offs;
and explain not only **what** a solution is but **why** it is correct.
