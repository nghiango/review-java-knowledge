# Reliability Issues

Failure handling, configuration, deployment and testing weaknesses that reduce dependable service.

## Entries

### Unchecked Optional access

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Calling `Optional.get()` without proving presence converts expected absence into a context-free
exception. Map absence explicitly with `orElseThrow` or transform it with `map`/`flatMap`.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Broad exception catch hides an outage

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Catching `Exception` around repository access merges absence, invalid input, defects and outages.
Handle only the failure the layer owns; preserve operational failures and their causes.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Structural mutation invalidates iteration

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Removing through a collection while an enhanced-for Iterator is active changes its modification
count and fails fast. Use iterator removal, `removeIf`, or derive a new immutable result.

### Abrupt JVM Termination Drops In-Flight Tasks

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Default `server.shutdown: immediate` and unmanaged `ExecutorService` instances forcefully terminate
running worker threads and sever active HTTP connections upon receiving `SIGTERM`. Configure
`server.shutdown: graceful` and manage worker threads via `ThreadPoolTaskExecutor` with
`setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)`.

**Appears in:** `modules/05-spring-boot/broken-examples/no-graceful-shutdown`

### Assertions coupled to implementation details

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5, Mockito · **Interview frequency:** High · **Production impact:** High

Verifying call order, call counts and exact argument lists restates the implementation instead of the
observable outcome. The test then fails on behaviour-preserving refactors and still passes when the
business result is wrong. Assert the returned value or a captured side effect.

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### Mocking a value collaborator removes the rule under test

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Mockito · **Interview frequency:** High · **Production impact:** High

Doubling a pure, in-process collaborator and stubbing its result deletes the code path the test
claims to verify; the test can only prove that the stub was called. Use the real collaborator and
reserve test doubles for I/O boundaries, time and randomness.

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### Mocking the HTTP boundary hides contract drift

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Mockito, WireMock · **Interview frequency:** High · **Production impact:** High

Doubling the outbound HTTP client makes the suite green while the real request path and JSON field
names are never exercised, so a renamed endpoint or field ships undetected. Replace the boundary with
a stub server (WireMock) and assert the recorded request and the deserialized response.

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### No assertion on the wire contract

**Type:** Testing issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** WireMock, Spring `RestClient` · **Interview frequency:** Medium · **Production impact:** Medium

A test that stubs the client cannot verify the URL path, the `Accept` header or that the response body
deserializes into the expected type. Against a stub server, assert `verify(getRequestedFor(...))` with
the header and let Jackson bind the real payload.

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### No timeout or error-path coverage on an outbound call

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring `RestClient` · **Interview frequency:** High · **Production impact:** High

An outbound HTTP client built without connect or read timeouts, tested only on the happy path, blocks a
request thread for the container's default and surfaces a 5xx or malformed body as a raw exception. Set
explicit timeouts and test the failure paths (timeout, 5xx, unparseable body).

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### Tests share a mutable static fixture

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5 · **Interview frequency:** High · **Production impact:** High

A static mutable collection of test data makes one test's output another test's input: the result depends
on what ran before, failures are attributed to the wrong test, and a broken unit can stay green on data a
previous test left behind. Build the data a test asserts on inside that test, and share only immutable
values or genuinely expensive started resources.

**Appears in:** `modules/12-testing/broken-examples/shared-mutable-test-fixtures`

### Test order dependence hidden by an explicit method order

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5 `@TestMethodOrder`, `@Order` · **Interview frequency:** High · **Production impact:** High

`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` with `@Order(n)` pins the execution order and
conceals a test that reads state another test wrote. The class is green, but the method fails when run
alone, under a random orderer or in parallel. Remove the shared state instead of ordering around it, and
express a real shared-resource dependency once with `@BeforeAll` or an extension.

**Appears in:** `modules/12-testing/broken-examples/shared-mutable-test-fixtures`

### Sleep-based waiting for asynchronous work

**Type:** Testing issue · **Severity:** High · **Difficulty:** Basic

**Technology:** JUnit 5, Awaitility · **Interview frequency:** High · **Production impact:** High

A fixed `Thread.sleep` before asserting on a background result guesses how long the work takes instead
of waiting for the condition: the test fails on a loaded machine, passes on a fast one, and passes even
if the work ran synchronously, so the asynchronous contract is never verified. Poll the state under
test with Awaitility (`atMost` + `untilAsserted`) so the test returns as soon as the condition holds
and fails with the value it actually observed.

**Appears in:** `modules/12-testing/broken-examples/sleep-based-async-assertions`

### Unbounded polling loop masks a hang

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5, `Thread.sleep` polling · **Interview frequency:** High · **Production impact:** High

A `while (status != EXPECTED) { Thread.sleep(...); }` loop has no timeout and no exit for any other
terminal state, so a report that fails or hangs keeps the loop spinning until the CI job timeout kills
the build — no assertion message, no observed status, and the run is written off as infrastructure
flake. Replace the loop with a bounded `await().atMost(...).untilAsserted(...)` that reports the state
it saw.

**Appears in:** `modules/12-testing/broken-examples/sleep-based-async-assertions`

### No failure-path assertion on an asynchronous job

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5, asynchronous job lifecycle · **Interview frequency:** High · **Production impact:** High

A suite that only asserts the happy-path status never executes the branch that records failure, so a job
that swallows the exception and stays `RUNNING` (or reports `COMPLETED`) ships green while callers poll
for ever and nothing is retried or alerted. Make the work throw through an injectable seam and assert the
terminal failure state.

**Appears in:** `modules/12-testing/broken-examples/sleep-based-async-assertions`

### Embedded substitute diverges from production semantics

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring Data JPA, PostgreSQL, test doubles · **Interview frequency:** High · **Production impact:** High

A hand-written in-memory repository replaces the real store, so the suite asserts the substitute's
matching, ordering and constraint behaviour instead of PostgreSQL's. The fake answers lookups
case-insensitively, returns insertion order and overwrites duplicates silently; PostgreSQL compares
case-sensitively, guarantees no row order without a `Sort`, and rejects a duplicate with a constraint
violation. The service ships green against a store it never runs on.

**Appears in:** `modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics`

### Duplicate rule enforced only by a fake that cannot fail

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring Data JPA, PostgreSQL · **Interview frequency:** High · **Production impact:** High

A read-then-write uniqueness check backed only by a substitute that cannot raise a constraint
violation: concurrent registrations both pass the check, and the failure the database will produce is
never exercised. Put the rule in the database as a unique constraint, translate its violation into a
domain error, and test the conflicting write against the real engine.

**Appears in:** `modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics`

### Absolute assertions pinned to the declaration order

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5 `@TestMethodOrder`, `@Order` · **Interview frequency:** High · **Production impact:** High

Each method of the class asserts the absolute value it expects to be handed out next from a shared
counter, so those values are positions in one sequence rather than properties of the code under test.
`@TestMethodOrder(OrderAnnotation.class)` supplies an execution order that makes them line up, and the
class stays green until a method is run alone, reordered or picked by a random orderer. Create the
state a test asserts on inside that test, and assert consequences of the calls the test itself makes.

**Appears in:** `modules/12-testing/broken-examples/order-dependent-test-suite`

### Static counter shared across allocators

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5 static state · **Interview frequency:** High · **Production impact:** High

A counter kept in production static state belongs to the class, not to an allocator: every instance
shares one sequence, and a newly constructed allocator does not start from the value it was given but
continues wherever the process already is. Tests that build a fresh instance per method still share the
counter with each other, with other test classes and with previous runs in the same JVM, so what one
method observes depends on how many times the methods before it called `next()`. Keep mutable state in
instance fields seeded from the constructor argument, and reserve static state for genuinely process-wide
services that are modelled explicitly.

**Appears in:** `modules/12-testing/broken-examples/order-dependent-test-suite`

### Infinite TTL on Dynamic Keys Leading to Redis OOM

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Intermediate

Writing high-cardinality keys (such as user session tokens or search queries) without an expiration TTL causes unbounded memory growth in Redis. Under the default `noeviction` policy, Redis rejects all mutating writes once `maxmemory` is reached. Under `volatile-lru`, immortal keys without TTL are exempt from eviction, causing the eviction of unrelated TTL-protected caches. Enforce mandatory TTLs on all writes.

**Appears in:** `modules/13-caching-redis/broken-examples/infinite-ttl-memory-leak`

### Cache Penetration on Non-Existent Keys

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

When an entity is not found in persistent storage, returning `null` without caching a sentinel value causes repeated requests for invalid or malicious IDs to bypass the caching layer entirely and query the primary database every time, exposing the database to denial of service. Cache a sentinel null object with a short TTL or filter with an in-memory Bloom filter.

**Appears in:** `modules/13-caching-redis/broken-examples/caching-null-and-exceptions`

---

### Feign Default ErrorDecoder Masking 4xx Client Errors into Generic 500s

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** OpenFeign, Spring Cloud · **Interview frequency:** High · **Production impact:** High

OpenFeign's default `ErrorDecoder` converts all non-2xx HTTP responses into generic runtime `FeignException` instances without domain translation. Expected business conditions such as 404 (Item Not Found) or 409/422 (Insufficient Stock) bubble up unhandled and trigger generic HTTP 500 Internal Server Error responses to end users. Implement a custom `ErrorDecoder` that maps 4xx status codes into explicit domain exceptions and marks transient 503/504 errors as `RetryableException`.

**Appears in:** [Spring Cloud — Feign missing error decoder](../topics/spring-cloud/code-review.md#openfeign-missing-timeouts-and-custom-error-decoder)

---

### Missing Timeout Protection on Parallel Reactive Publishers in Mono.zip

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring WebFlux, Project Reactor · **Interview frequency:** High · **Production impact:** Critical

`Mono.zip` waits for all combined inner publishers to complete before producing its tuple result. If any of the combined publishers lacks an explicit timeout, a single hanging remote service stalls the entire composite response indefinitely. Each parallel publisher orchestrated by `Mono.zip` must define an individual bounded `.timeout(Duration)` and fallback behavior.

**Appears in:** `modules/21-webclient-webflux/broken-examples/chain-without-error-handling`

---

### Silent Failure Suppression Masks Persistent Data Drift

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Exception Handling, Data Integrity · **Interview frequency:** High · **Production impact:** High

Catching checked or unchecked exceptions inside database update or reconciliation pipelines and returning a generic `false` without re-throwing or raising domain alerts conceals persistent data corruption from transactional callers. Because the method does not throw, the outer transaction commits successfully, leaving accounting books or inventory ledgers in an inconsistent state. Always propagate domain exceptions or return explicit, strongly-typed failure results.

**Appears in:** `modules/22-observability/broken-examples/swallowed-exceptions-observability`

## Related

- [Issue catalogue](index.md)
