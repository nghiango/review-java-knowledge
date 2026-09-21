# Maintainability and Design Issues

Coupling, weak abstractions, API contracts, encapsulation and architecture failures.

## Entries

### Exposed mutable collection

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** Core Java collections · **Interview frequency:** Medium · **Production impact:** Medium

Storing and returning a caller-owned list aliases mutable state across boundaries. Use `List.copyOf`
when taking a snapshot; an unmodifiable view alone still reflects mutation through the original list.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Extensible value-object equality

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java equality · **Interview frequency:** High · **Production impact:** High

Open inheritance and value equality can violate symmetry/transitivity when a subtype adds state.
Prefer a final immutable value type (a record where appropriate) or explicitly define identity
semantics for the hierarchy.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Inconsistent equals and hashCode state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** HashMap, HashSet · **Interview frequency:** High · **Production impact:** High

`equals` and `hashCode` must use compatible state: equal values must always have equal hashes.
Keep display/profile data outside the identity key and test the implication directly.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Optional as mutable shared state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

An Optional field on a singleton service is mutable cross-request state, not an absence contract.
Keep the service stateless or use an explicitly keyed, thread-safe cache.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Optional parameter obscures a required contract

**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic

An Optional parameter forces wrapping while still allowing a null Optional reference. Accept the
required value and validate it at the boundary; reserve Optional primarily for return absence.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Import operation couples I/O, parsing and mutation

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

When one method owns resource acquisition, syntax, validation and caller mutation, failure policy
cannot be tested or changed independently. Inject a source and return an explicit immutable result.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Duplicate listener registration

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Basic

A subscription API that does not model registration identity can retain and invoke the same listener
multiple times after retries or repeated startup hooks. Return a registration handle and define the
duplicate policy explicitly.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Listener backing list escapes

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

Returning a registry's mutable listener list lets callers change global state outside the registry
contract. Return an immutable snapshot for inspection.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Ambient request context lacks scope

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

An API that only sets ThreadLocal state hides ownership and cleanup. Return a closeable scope that
captures the previous value, restores parents and removes the value when no parent exists.

**Appears in:** `modules/02-jvm/broken-examples/threadlocal-pool-leak`

### Hidden static cache policy

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

Static cache policy cannot be configured, replaced or tested at the component boundary. Make the
capacity and eviction policy explicit, or inject a production cache implementation.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Chained regex character replacement

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

Chaining multiple `replaceAll` calls repeatedly parses regex patterns and allocates intermediate
string instances for each replacement. Prefer a single-pass character scan using a `StringBuilder`.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Untyped loose domain arguments

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

Passing loose primitives and mutable maps bypasses validation at domain construction time and allows
unvalidated or concurrently modified state. Model domain telemetry payloads as immutable records.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Field injection hides dependencies and prevents unit testing

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Basic

Injecting dependencies directly into private fields via `@Autowired` hides constructor contracts, prevents `final` immutable fields, and makes unit testing impossible without reflection or Spring runners. Use explicit constructor injection.

**Appears in:** [Spring Core — circular field injection](../topics/spring-core/code-review.md#circular-field-injection)

### Circular dependency between domain services

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Intermediate

Direct bidirectional dependencies between beans (`ServiceA` $\leftrightarrow$ `ServiceB`) prevent isolated reasoning and trigger startup failures in modern Spring Boot. Decouple services using domain events (`ApplicationEventPublisher` / `@EventListener`).

**Appears in:** [Spring Core — circular field injection](../topics/spring-core/code-review.md#circular-field-injection)

### Self-invocation bypasses Spring AOP proxy interceptors

**Type:** Architecture issue · **Severity:** Critical · **Difficulty:** Intermediate

Calling an annotated method (`@Transactional`, `@Audited`, `@Async`, `@Cacheable`) on `this` within the same class stays inside the target instance and bypasses the Spring proxy interceptor chain, silently omitting cross-cutting behavior. Move the annotated method to a dedicated collaborator bean.

**Appears in:** [Spring Core — self-invocation aspect bypass](../topics/spring-core/code-review.md#self-invocation-aspect-bypass)

### Prototype bean injected into singleton is never re-created

**Type:** Scope issue · **Severity:** Critical · **Difficulty:** Intermediate

Injecting a `@Scope("prototype")` bean directly into a singleton constructor resolves and wires the prototype only once at container startup. Use `ObjectProvider<T>`, `@Lookup`, or `ScopedProxyMode.TARGET_CLASS` to dynamically fetch fresh instances.

**Appears in:** [Spring Core — prototype injection in singleton](../topics/spring-core/code-review.md#prototype-injection-in-singleton)

### Mutable instance state in Spring singleton bean

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Spring `@Service` and `@Component` beans are shared singletons. Storing per-request data in instance fields causes multithreaded race conditions and cross-tenant data leakage. Keep services strictly stateless and pass data via method parameters and records.

**Appears in:** [Spring Core — mutable singleton state](../topics/spring-core/code-review.md#mutable-singleton-state)

### Scattered `@Value` configuration without validation

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

Direct `@Value` injection scatters property keys across services without type hierarchy, IDE autocomplete, or startup validation. Encapsulate properties in `@ConfigurationProperties` records annotated with `@Validated` and Jakarta Bean Validation constraints.

**Appears in:** `modules/05-spring-boot/broken-examples/scattered-value-config`

### Accidental auto-configuration override bypasses framework customization

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Senior

Defining raw custom beans without using auto-configured builders or customizer callbacks accidentally overrides Spring Boot framework defaults, discarding Micrometer metrics, distributed tracing, and connection pool configuration. Use `@ConditionalOnMissingBean` and implement framework Customizer interfaces.

**Appears in:** `modules/05-spring-boot/broken-examples/accidental-autoconfig-override`

### God Controller with Embedded Business Logic

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

Embedding pricing algorithms, discount rules, external payment gateway calls, and database transactions inside HTTP controllers violates Single Responsibility and impairs testability. Extract domain logic into dedicated domain services and keep controllers thin.

**Appears in:** `modules/06-spring-mvc/broken-examples/god-controller-business-logic`

### Wrong HTTP Status Codes and Custom Envelopes

**Type:** Architecture issue · **Severity:** Medium · **Difficulty:** Basic

Returning universal `200 OK` responses with buried error messages or lacking `Location` headers on resource creation misleads caching proxies, API gateways, and monitoring dashboards. Use standard semantic HTTP status codes (`201 Created`, `204 No Content`, `400 Bad Request`, `404 Not Found`).

**Appears in:** `modules/06-spring-mvc/broken-examples/wrong-http-status-codes`

### Service boundary with no observable behaviour

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5, Mockito · **Interview frequency:** High · **Production impact:** High

When the outcome of a use case is only expressed as an outbound call, tests cannot pin it down and
match it with `any()`. Return the value the caller depends on (or capture the side effect) so the
boundary has a contract that a test can assert — otherwise a wrong charge or wrong total ships
green.

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### Test coupled to internal call structure

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** Mockito `InOrder` · **Interview frequency:** Medium · **Production impact:** Medium

`InOrder` and `times(n)` verification encode today's control flow, so extract-method and reorder
refactors fail without any behaviour change. Reserve order verification for sequences that are
themselves the contract (authenticate before authorise, commit before ack).

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### HTTP client interface leaks the wire format

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** Spring `RestClient`, Jackson · **Interview frequency:** Medium · **Production impact:** Medium

Returning the raw JSON body as an untyped `Map` forces every caller to know the field names and cast
the values, so a wire-format change ripples through the domain layer and nulls surface as NPEs far from
the boundary. Return a typed record and let the client own (de)serialization.

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### No per-test data builder; a test edits a shared fixture

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** JUnit 5 test data builders · **Interview frequency:** Medium · **Production impact:** Medium

Without a builder, a test that needs different data mutates the shared fixture and reads its own entry
back. What the test depends on becomes implicit, the suite must be read in execution order, and changing
the fixture breaks tests that never mentioned it. Give each test a fluent builder that returns a fresh
value stating exactly the input the test needs.

**Appears in:** `modules/12-testing/broken-examples/shared-mutable-test-fixtures`

### Test fixture has no ownership or lifecycle

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** JUnit 5 static fixtures · **Interview frequency:** Medium · **Production impact:** Medium

A `static final` collection of test data protects the reference, not the contents: it lives for the whole
JVM, any test in the package can change it, and nothing resets it between tests — so adding one test file
changes another file's outcome. Model fixtures as factories that return fresh immutable values, and share
only started resources with an explicit lifecycle.

**Appears in:** `modules/12-testing/broken-examples/shared-mutable-test-fixtures`

### Fixed delays slow the suite and encode machine speed

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** JUnit 5, `Duration` constants · **Interview frequency:** Medium · **Production impact:** Medium

Hard-coded sleeps make every test pay the full simulated duration and copy the implementation's timing
into the test: change the production constant and the tests fail, shorten it and the sleeps stay behind,
and each run buys seconds of dead wall-clock time. Wait on the condition with a bound instead, and release
the work where the transition itself is what the test is about.

**Appears in:** `modules/12-testing/broken-examples/sleep-based-async-assertions`

### Test double duplicates the repository contract

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** Spring Data JPA test doubles · **Interview frequency:** Medium · **Production impact:** Medium

An `InMemory…Repository` in the test sources is a second implementation of the repository contract,
with its own persistence semantics, kept in step with the real interface by hand and owned by nobody.
Change a query or a constraint and only the fake — which the tests read — is now wrong. Reserve test
doubles for interactions, and test the store's semantics against the store.

**Appears in:** `modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics`

### Order-dependent test class cannot run in parallel

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** JUnit 5 parallel execution · **Interview frequency:** Medium · **Production impact:** Medium

A class whose methods share a counter and assert absolute values can only be executed as one ordered
run: it cannot be split, reordered, selected method-by-method, sharded across CI agents or run with
JUnit's parallel execution enabled. The cost appears only once the build is optimised — and by then the
suite is treated as correct, so parallelism is switched off again "because the suite is flaky". Keep
the tests independent instead of ordering around the coupling.

**Appears in:** `modules/12-testing/broken-examples/order-dependent-test-suite`

---

### Breaking Reactive Chain with .block() Forfeits Backpressure and Cancellation

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Project Reactor, Reactive Streams · **Interview frequency:** High · **Production impact:** High

Breaking a reactive stream by invoking `.block()` or `toFuture().get()` decouples the downstream consumer from upstream publishers. This eliminates the Reactive Streams demand signaling mechanism (backpressure), breaks distributed tracing context propagation across operators, and prevents client cancellations (such as closed HTTP browser connections) from propagating upstream to abort redundant backend processing. Preserve end-to-end reactive composition using `.flatMap()` and `.map()`.

**Appears in:** `modules/21-webclient-webflux/broken-examples/block-in-request-flow`

### Caller-Controlled Performance Work Limit

**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** Pagination · **Interview frequency:** Medium · **Production impact:** Medium

Passing an arbitrary limit to persistence lets one request select an unsafe amount of work. Enforce
a service maximum and require pagination to keep a stable memory and latency envelope.

**Appears in:** `modules/23-performance/broken-examples/n-plus-one-summary`

---

### Bypassing Test Verification on Main Deployment Branch

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** CI/CD, Gradle, Software Quality Gates · **Interview frequency:** High · **Production impact:** Critical

Skipping test tasks (`-x test`) during packaging on the `main` trunk branch operates on the flawed assumption that feature branches were sufficient. Sequential merges into `main` frequently introduce semantic conflicts, broken database expectations, or incompatible dependency updates that are never validated prior to deployment. Every artifact released to production must be built and verified against the exact commit SHA on the target branch.

**Appears in:** `modules/26-ci-cd/broken-examples/tests-and-security-skipped-on-main`

---

### Domain Model Coupled Directly to Infrastructure and Framework Annotations

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Hexagonal Architecture, Clean Architecture, DDD · **Interview frequency:** High · **Production impact:** High

Annotating core business entities with framework and database annotations (`@Entity`, `@Table`, `@Component`, `@Autowired`) couples domain logic to infrastructure frameworks and persistence lifecycles. Domain models cannot be unit tested in isolation without Spring or persistence runners, and upgrading or swapping infrastructure frameworks requires intrusive refactoring of core business logic. In Hexagonal and Clean Architecture, domain models must remain pure POJOs depending only on inward contracts.

**Appears in:** `modules/28-architecture/broken-examples/domain-depending-on-infrastructure`

---

### Anaemic Domain Model with Exposed Setters Leaking Business Invariants

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Domain-Driven Design, Object-Oriented Design · **Interview frequency:** High · **Production impact:** High

Treating domain entities as passive data holders with public getters and setters forces business invariants, validation rules, and lifecycle transitions into procedural god services. Callers across the application can mutate fields into corrupt or inconsistent states without triggering recalculation or state machine checks. Encapsulate business logic within rich Aggregate Roots and protect internal collections using unmodifiable views.

**Appears in:** `modules/28-architecture/broken-examples/anaemic-domain-with-god-service`

---

### Cross-Module Database Access Violating Bounded Context Isolation

**Type:** Maintainability issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Modular Monolith, Bounded Contexts, Microservices · **Interview frequency:** High · **Production impact:** Critical

Allowing one module in a modular monolith or microservices architecture to directly query or mutate database tables owned by another bounded context violates encapsulation. Changes to internal schemas immediately break foreign modules, domain lifecycle logic is bypassed, and database-level deadlocks emerge. Modules must interact strictly through published Java API contracts or asynchronous Domain Events.

### Procedural Switch-on-Type Growth Violating Open/Closed Principle

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Java Switch, Strategy Pattern, SOLID Principles · **Interview frequency:** High · **Production impact:** High

Using monolithic `switch` statements across multiple methods to handle domain type variants forces modification of existing, tested code whenever a new variant is introduced. As new types are added, cyclomatic complexity explodes, merge conflicts multiply across teams, and omitting a case branch triggers runtime exceptions instead of compile-time errors. Refactor to polymorphic interfaces (Strategy Pattern) with dynamic registry discovery or Java 21 sealed hierarchies with exhaustive switch expressions.

**Appears in:** `modules/29-design-patterns/broken-examples/switch-on-type-growth`

---

### Premature Pattern Over-Engineering Violating KISS and YAGNI

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** Gang of Four Patterns, Clean Code, Refactoring · **Interview frequency:** High · **Production impact:** Medium

Stacking multiple structural and behavioral design patterns (such as Bridge, Abstract Factory, and Visitor) to solve straightforward requirements (e.g. formatting a flat list of records into CSV) introduces excessive cognitive overhead, obscures data flow behind layers of synthetic delegation, and creates unnecessary heap allocations and garbage collection pressure. Always favor idiomatic language constructs, static helpers, or standard library features until genuine variability requirements emerge.

**Appears in:** `modules/29-design-patterns/broken-examples/pattern-over-engineering`

## Related

- [Issue catalogue](index.md)
