# Testing Tests

The module's own suite is the worked example of the lesson: fast, deterministic unit tests that run
under `build` with no Docker, and a small set of integration tests that exercise the semantics only
real infrastructure can prove. This page separates the two, lists the exact commands,
and documents the shared-container contract.

## What each level proves

| Level | Source set | Runs under | Needs Docker | Proves |
|---|---|---|---|---|
| Unit | `src/test` | `./gradlew :modules:12-testing:test` (and `build`) | No | Behaviour of pure collaborators and orchestration: pricing rules, order totals, the async lifecycle, sequence allocation |
| Slice | `src/integrationTest` (`@DataJpaTest`) | `./gradlew :modules:12-testing:integrationTest` | Yes | PostgreSQL semantics: unique index, collation, row ordering, flush/clear visibility |
| Contract | `src/integrationTest` (WireMock) | `./gradlew :modules:12-testing:integrationTest` | No (stub server) | The outbound HTTP wire contract: request path, `Accept` header, JSON deserialization |
| Review targets | `broken-examples/` | `./gradlew :modules:12-testing:compileBrokenExamples` | No | That the review targets are real, compilable code — they are never run |
| Question examples | `src/examples` | `./gradlew :modules:12-testing:compileExamples` | No | That the classes shown in the question bank still compile against the real test APIs |

## Running the suites

```bash
./gradlew :modules:12-testing:test                  # fast unit tests, no Docker
./gradlew :modules:12-testing:integrationTest       # Testcontainers / WireMock (Docker for the JPA slice)
./gradlew :modules:12-testing:compileExamples       # compile the question/demo examples
./gradlew :modules:12-testing:compileBrokenExamples # compile the review targets
```

`./gradlew build` runs the unit tests only, so the default local and CI build **never needs Docker**.
The integration tests are a separate task and a separate source set; nothing in `src/main`, `src/test`,
`src/examples` or `broken-examples/` depends on a container.

## Deterministic unit tests (no Docker)

| Test class | Invariant proved | Technique |
|---|---|---|
| `CheckoutServiceBehaviourTest` | The customer is charged the discounted total; empty and fully discounted baskets charge nothing; invalid input is rejected before payment | Real `PricingCalculator`, mocked `PaymentGateway`, AssertJ on the returned and charged amount |
| `OrderTotalsTest` | Line totals, multi-line sums, shipping, and that built orders are independent and immutable | `OrderTestData` builders, no shared fixture, no `@TestMethodOrder` |
| `AsyncReportJobTest` | The `QUEUED` → `RUNNING` → `COMPLETED`/`FAILED` lifecycle, driven by a released latch and a failing work seam | Awaitility `atMost` + `untilAsserted`, single-thread executor |
| `SequenceAllocatorTest` | Consecutive allocation, `current()`, `reset()`, instance independence, and failure at `Long.MAX_VALUE` | One allocator per test, no static state |

### Proving behaviour rather than implementation

```java
--8<-- "modules/12-testing/src/test/java/lab/testing/pricing/CheckoutServiceBehaviourTest.java"
```

### Proving the async lifecycle without a sleep

```java
--8<-- "modules/12-testing/src/test/java/lab/testing/async/AsyncReportJobTest.java"
```

### Proving per-test data independence

```java
--8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTestData.java"
```

```java
--8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTotalsTest.java"
```

### Proving order independence

```java
--8<-- "modules/12-testing/src/test/java/lab/testing/sequence/SequenceAllocatorTest.java"
```

## Integration tests (Docker for the PostgreSQL slice)

| Test class | Invariant proved | Technique |
|---|---|---|
| `AccountRepositoryIT` | The unique index on `email` is case-sensitive and rejects an exact duplicate; `findByEmail` is case-sensitive; row order requires a `Sort`; `AccountService` normalises the email; balance reflects persisted state after flush/clear | `@DataJpaTest` with `Replace.NONE`, `@ServiceConnection` container, `@TestConstructor` |
| `OrderFulfilmentWireMockIT` | The client calls `GET /inventory/{sku}` with `Accept: application/json`, binds the response, and returns the right answer for available stock equal to or below the request | WireMock extension, real `RestClient` with explicit timeouts |

### Proving PostgreSQL semantics against the real engine

```java
--8<-- "modules/12-testing/src/integrationTest/java/lab/testing/accounts/TestingJpaConfiguration.java"
```

```java
--8<-- "modules/12-testing/src/integrationTest/java/lab/testing/accounts/AccountRepositoryIT.java"
```

### Proving the wire contract against a stub server

```java
--8<-- "modules/12-testing/src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java"
```

### The `test-support` shared-container contract

Every Docker-backed test in the baseline shares one PostgreSQL container per JVM through
`modules/test-support`:

```java
--8<-- "modules/test-support/src/main/java/lab/testsupport/SharedPostgresContainer.java"
```

The contract a consumer must respect:

- **One dependency.** `integrationTestImplementation(project(":modules:test-support"))` — the
  Testcontainers artifacts are exposed with `api`, so the consumer inherits `PostgreSQLContainer` and
  the JUnit 5 extension without redeclaring them.
- **Start it yourself, do not manage it.** `SharedPostgresContainer.instance()` starts the container on
  first use and returns the same instance afterwards, so a consumer must **not** annotate it with
  `@Container`/`@Testcontainers` — that would try to manage a second lifecycle.
- **Publish it as a `@ServiceConnection` bean.** A `@TestConfiguration` returns
  `SharedPostgresContainer.instance()` from a `@Bean` annotated `@ServiceConnection`, and Spring Boot
  derives the DataSource (URL, username, password, driver) from the running container instead of a
  hand-written `@DynamicPropertySource`.
- **No embedded substitute.** The JPA slice sets `@AutoConfigureTestDatabase(replace = Replace.NONE)`
  so Boot cannot silently swap the container-backed DataSource for H2; the image is pinned to
  `postgres:17-alpine` so every module tests the same major version.
- **Docker-free build.** `test-support` contains no test that needs Docker, so `./gradlew build` stays
  Docker-free; the helper is exercised only by a consumer's `integrationTest` source set.

### Slice bootstrap

`@DataJpaTest` needs a `@SpringBootConfiguration` it can discover by walking up from the test package;
`lab.testing.TestingApplication` provides one so the slice's entity and repository scans start at
`lab.testing` and cover `lab.testing.accounts`. It is not a runnable application — no `main` method and
no web server.

## CI implications

- **`build` is Docker-free.** Unit tests, `compileExamples` and `compileBrokenExamples` never touch a
  container, so a CI job without a Docker daemon can still run the whole build and the review-target
  compile.
- **`integrationTest` needs Docker (and Ryuk).** The JPA slice starts a real PostgreSQL container; the
  container is reaped by Testcontainers' Ryuk when the JVM exits, so the CI runner must allow the
  daemon socket and the Ryuk connection. Without a daemon the task fails at container start, not at
  assertion time.
- **One container per JVM, not per class.** Sharing the started container keeps `integrationTest` fast;
  the price is that tests must not assume an empty database — the slice uses `create-drop` and the
  assertions are written for the rows each test inserts.
- **WireMock needs no daemon.** The contract test runs a stub server on a dynamic port inside the JVM,
  so it is Docker-free and can run in the same job as the unit tests.
- **Version pinning.** `postgres:17-alpine` and the Testcontainers Docker API version are pinned, so a
  runner with a newer daemon does not silently change the engine the semantics are asserted against.

## Related

- [Solutions](solutions.md)
- [Code review](code-review.md)
- [Production](production.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
