# Testing Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Module 12 — Testing with six production-realistic review exercises, the repository's first Docker-backed integration-test infrastructure, full documentation and hidden Q&A answers.

**Architecture:** Six broken examples each pair a flawed review target with a correct, tested counterpart. Three examples are flawed *test* code (implementation-coupled assertions, mock-heavy tests, sleep-based waiting, shared fixtures, order dependence) whose correct counterparts live in `src/main` plus corrected tests in `src/test`. The PostgreSQL example introduces `modules/test-support` (shared container singleton) and the first `src/integrationTest` suite.

**Tech Stack:** Java 21, Gradle 9.7.1 Kotlin DSL, JUnit Jupiter 5.14.4, AssertJ 3.27.7, Mockito 5.15.2, Awaitility 4.2.2, Testcontainers 1.20.6, WireMock 3.12.1, ArchUnit 1.4.0, PostgreSQL 42.7.5, Spring Boot 3.4.3 test slices, MkDocs Material.

**Spec:** `docs/spec/curriculum.md` §12, `docs/spec/module-conventions.md`, `docs/spec/issue-format.md`, `docs/spec/documentation-conventions.md`.

## Global Constraints

- Baseline stack only: Java 21, Spring Boot 3.4.3, existing version catalog. All new test dependencies are already approved in `AGENTS.md` §1 (Testcontainers, WireMock, Awaitility, ArchUnit, PostgreSQL).
- H2 is banned. The curriculum's "H2 hiding PostgreSQL behaviour" example is implemented as an in-memory substitute whose semantics diverge from PostgreSQL; the H2 manifestation is documented, never added as a dependency.
- `./gradlew build` must not require Docker. Only `./gradlew integrationTest` may use Testcontainers.
- No `Thread.sleep` in any test; asynchronous assertions use Awaitility with a bounded timeout.
- Broken review targets contain no categorized issue comments; `SOLUTION.md` is the only place they appear.
- Every issue uses an exact category string from `issue-format.md` §3 and is registered in the §6 catalogue file (`Testing issue` → `reliability.md`, `Design issue`/`Maintainability issue` → `maintainability.md`, `Database issue` → `database.md`).
- Every question has exactly one collapsed `??? question "Reveal answer"` and a nested collapsed `??? example "Example"`; each question owns one dedicated class under `src/examples/java/lab/testing/questions/` with trailing inline result comments.
- Java shown in docs comes from checked `--8<--` snippets, never pasted copies.
- MkDocs must stay `--strict` green after every task: page stubs and nav entries land together in Task 2.
- Production code is test-first; a test must be observed failing before the implementation exists.

---

### Task 1: Add approved test-infrastructure dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`

**Produces:** catalog entries consumed by Tasks 4, 6, 7.

- [ ] Add versions: `testcontainers = "1.20.6"`, `awaitility = "4.2.2"`, `wiremock = "3.12.1"`, `archunit = "1.4.0"`, `postgresql = "42.7.5"`.
- [ ] Add libraries: `testcontainers-junit-jupiter` (`org.testcontainers:junit-jupiter`), `testcontainers-postgresql` (`org.testcontainers:postgresql`), `spring-boot-testcontainers` (`org.springframework.boot:spring-boot-testcontainers`, `version.ref = "spring-boot"`), `spring-boot-test-autoconfigure` (`org.springframework.boot:spring-boot-test-autoconfigure`, `version.ref = "spring-boot"`), `awaitility` (`org.awaitility:awaitility`), `wiremock-standalone` (`org.wiremock:wiremock-standalone`), `archunit-junit5` (`com.tngtech.archunit:archunit-junit5`), `postgresql` (`org.postgresql:postgresql`).
- [ ] Run `./gradlew build` and confirm the catalog still resolves; run `./gradlew :modules:11-spring-security:dependencies --configuration testRuntimeClasspath` to confirm no resolution breakage.
- [ ] Commit: `Add approved test-infrastructure dependencies`.

---

### Task 2: Scaffold Module 12 with docs stubs, nav and progress wiring

**Files:**
- Modify: `settings.gradle.kts`
- Create: `modules/12-testing/{README.md,build.gradle.kts}`
- Create stub pages: `docs/topics/testing/{index,concepts,internals,questions,code-review,solutions,tests,production,exercises}.md`
- Create stub: `docs/questions/testing.md`
- Modify: `mkdocs.yml`, `docs/progress.md`, `docs/interview-checklist.md`

**Interfaces:** package root `lab.testing`; Gradle project `:modules:12-testing`.

- [ ] `include("modules:12-testing")`; module applies only `lab.java-conventions`.
- [ ] Module dependencies: `implementation` spring-context, spring-tx, spring-jdbc, spring-web, spring-data-jpa, hibernate-core, hikari-cp, jakarta-persistence-api, spring-boot, spring-boot-autoconfigure, jackson-databind, jakarta-validation-api; `testImplementation` spring-test, spring-boot-test, spring-boot-test-autoconfigure, mockito-core, mockito-junit-jupiter, awaitility, archunit-junit5, wiremock-standalone; `integrationTestImplementation` testcontainers-junit-jupiter, testcontainers-postgresql, spring-boot-testcontainers, postgresql. Do not add `bootRun` (library module).
- [ ] `README.md` is a pointer: purpose, `test` / `integrationTest` / `compileExamples` / `compileBrokenExamples` commands, link to `docs/topics/testing/index.md`.
- [ ] Create the nine topic pages plus `docs/questions/testing.md` as minimal stubs containing a one-line placeholder and a `## Related` section, so nav can land before content. Each stub is replaced by Tasks 10–12.
- [ ] Add nav entries: a `Testing` topic block with all nine pages, `Questions → Testing: questions/testing.md`, and `Reference → Testing Plan: superpowers/plans/2026-09-20-testing-module.md`.
- [ ] Add a `## Testing` section to `docs/interview-checklist.md` covering the six exercises' lessons. Set the Module 12 row in `docs/progress.md` to 🟨 for Theory/Q&A/Broken/Correct/Tests/Scenario/Docs.
- [ ] Run `./gradlew :modules:12-testing:build` and `.venv/bin/mkdocs build --strict`.
- [ ] Commit: `Scaffold the testing module`.

---

### Task 3: Review exercise — assertions coupled to implementation

**Files:**
- Create: `modules/12-testing/broken-examples/asserting-implementation-not-behaviour/{CheckoutService.java,CheckoutServiceTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/pricing/{Money,LineItem,PricingCalculator,CheckoutService,PaymentGateway}.java`
- Create: `src/test/java/lab/testing/pricing/CheckoutServiceBehaviourTest.java`

**Interfaces:**
```java
public record Money(long cents) { public Money plus(Money other); public Money multiply(int factor); }
public record LineItem(String sku, long unitPriceCents, int quantity) {}
public final class PricingCalculator { public Money subtotal(List<LineItem> items); public Money applyPercentDiscount(Money amount, int percent); }
public final class CheckoutService { CheckoutService(PricingCalculator calculator, PaymentGateway gateway); public Money checkout(List<LineItem> items, int discountPercent); }
public interface PaymentGateway { void charge(Money amount); }
```

- [ ] RED: write `CheckoutServiceBehaviourTest` asserting observable behaviour — total for mixed quantities, discount rounding, empty basket, rejection of negative quantity — with `PricingCalculator` real and only `PaymentGateway` doubled. Observe failure (types absent).
- [ ] Implement `Money`, `LineItem`, `PricingCalculator`, `CheckoutService`, `PaymentGateway` minimally; run green.
- [ ] Broken target: `CheckoutServiceTest` mocks `PricingCalculator`, asserts exact argument lists, `verify` call ordering and `InOrder`, and asserts the internal `subtotal` call count — it passes even when the discount rule is wrong. `REVIEW.md` lists files and review dimensions; `SOLUTION.md` annotates issues and links to `lab.testing.pricing`.
- [ ] Issues to document (3–8): Testing issue (assertions coupled to implementation), Testing issue (mock of a value collaborator proves nothing), Design issue (no behaviour observable at the service boundary), Maintainability issue (test breaks on every refactor).
- [ ] Register issues in `docs/issues/reliability.md` and `docs/issues/maintainability.md` with back-links.
- [ ] Run `./gradlew :modules:12-testing:compileBrokenExamples :modules:12-testing:test`; commit: `Test checkout behaviour instead of implementation`.

---

### Task 4: Review exercise — mocking away the integration

**Files:**
- Create: `broken-examples/mocking-away-the-integration/{InventoryClient.java,OrderFulfilmentService.java,OrderFulfilmentServiceTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/fulfilment/{InventoryClient,InventoryResponse,OrderFulfilmentService}.java`
- Create: `src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java`

**Interfaces:**
```java
public record InventoryResponse(String sku, int available) {}
public final class InventoryClient { InventoryClient(RestClient client, String baseUrl); public InventoryResponse lookup(String sku); }
public final class OrderFulfilmentService { OrderFulfilmentService(InventoryClient client); public boolean canFulfil(String sku, int quantity); }
```

- [ ] RED: write `OrderFulfilmentWireMockIT` using `WireMockExtension`/`WireMockServer` on a dynamic port and a real `RestClient`; stub `GET /inventory/{sku}` returning `{"sku":"A-1","available":3}`; assert `canFulfil("A-1", 3)` is true and `canFulfil("A-1", 4)` is false; assert the recorded request path and `Accept` header. Observe failure.
- [ ] Implement the three classes; run `./gradlew :modules:12-testing:integrationTest` green.
- [ ] Broken target: the test mocks `InventoryClient` so a wrong path (`/stock/`) and wrong JSON field (`quantity`) pass unnoticed. `REVIEW.md` + `SOLUTION.md` with issue annotations.
- [ ] Issues: Testing issue (mocking the boundary hides contract drift), Testing issue (no serialization/HTTP path assertion), Reliability issue (no timeout or error-path coverage), Design issue (client interface leaks transport details).
- [ ] Register issues in `docs/issues/reliability.md` and `docs/issues/maintainability.md`.
- [ ] Run `compileBrokenExamples` and `integrationTest`; commit: `Verify HTTP contracts against a stub server`.

---

### Task 5: Review exercise — shared mutable fixtures

**Files:**
- Create: `broken-examples/shared-mutable-test-fixtures/{OrderFixture.java,OrderTotalsTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/orders/{Order,OrderLine,OrderTotals}.java`
- Create: `src/test/java/lab/testing/orders/{OrderTestData.java,OrderTotalsTest.java}`

**Interfaces:**
```java
public record OrderLine(String sku, long unitPriceCents, int quantity) {}
public record Order(String id, List<OrderLine> lines) {}
public final class OrderTotals { public Money total(Order order); public Money totalWithShipping(Order order, Money shipping); }
```

- [ ] RED: write `OrderTotalsTest` with per-test `OrderTestData` builders asserting totals, shipping, empty orders; observe failure.
- [ ] Implement the records plus `OrderTotals`; run green.
- [ ] Broken target: a static mutable `OrderFixture` list mutated by one test and relied on by another, with `@TestMethodOrder(OrderAnnotation.class)` masking the coupling. Issues: Testing issue (shared mutable fixture), Testing issue (order dependence), Maintainability issue (no builder, tests edit shared state), Design issue (fixture leaks between tests).
- [ ] Register issues in `docs/issues/reliability.md` and `docs/issues/maintainability.md`.
- [ ] Run module `test` + `compileBrokenExamples`; commit: `Isolate order fixtures per test`.

---

### Task 6: Review exercise — sleep-based asynchronous assertions

**Files:**
- Create: `broken-examples/sleep-based-async-assertions/{AsyncReportJob.java,AsyncReportJobTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/async/{ReportStatus,AsyncReportJob}.java`
- Create: `src/test/java/lab/testing/async/AsyncReportJobTest.java`

**Interfaces:**
```java
public enum ReportStatus { QUEUED, RUNNING, COMPLETED, FAILED }
public final class AsyncReportJob implements AutoCloseable {
    AsyncReportJob(ExecutorService executor, Duration simulatedWork);
    public ReportStatus submit(String reportId);
    public ReportStatus status(String reportId);
    @Override public void close();
}
```

- [ ] RED: write `AsyncReportJobTest` asserting the status transitions with `Awaitility.await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(25)).untilAsserted(...)`; assert a failure path reaches `FAILED`; observe failure.
- [ ] Implement `ReportStatus` and `AsyncReportJob` with a single-thread executor and no sleeps in production code; run green with `@AfterEach` closing the job.
- [ ] Broken target: the test calls `Thread.sleep(500)` and asserts once, so it is slow, flaky and silently passes on timeout. Issues: Testing issue (sleep-based waiting), Testing issue (unbounded wait masks a hang), Reliability issue (no failure-path assertion), Maintainability issue (fixed delays slow the suite).
- [ ] Register issues in `docs/issues/reliability.md`; commit: `Await asynchronous state without sleeping`.

---

### Task 7: Review exercise — embedded substitute hides PostgreSQL semantics, plus `test-support`

**Files:**
- Modify: `settings.gradle.kts` (`include("modules:test-support")`)
- Create: `modules/test-support/{build.gradle.kts,README.md}`
- Create: `modules/test-support/src/main/java/lab/testsupport/SharedPostgresContainer.java`
- Create: `broken-examples/embedded-substitute-hides-postgres-semantics/{InMemoryAccountRepository.java,AccountService.java,AccountServiceTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/accounts/{Account,AccountRepository,AccountService}.java`
- Create: `src/integrationTest/java/lab/testing/accounts/{TestingJpaConfiguration,AccountRepositoryIT}.java`
- Modify: `modules/12-testing/build.gradle.kts` (`integrationTestImplementation(project(":modules:test-support"))`)

**Interfaces:**
```java
// test-support
public final class SharedPostgresContainer { public static PostgreSQLContainer<?> instance(); }
// accounts
@Entity public class Account { Long id; String email; String displayName; long balanceCents; }
public interface AccountRepository extends JpaRepository<Account, Long> { Optional<Account> findByEmail(String email); }
public final class AccountService { AccountService(AccountRepository repository); public Account register(String email, String displayName); /* normalises email: trim + lowercase, then rejects duplicates explicitly */ public long balance(String email); }
```

- [ ] RED: write `AccountRepositoryIT` with `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = Replace.NONE)`, a `@SpringBootConfiguration` test application, and a `@TestConfiguration` exposing `@ServiceConnection PostgreSQLContainer<?>` from `SharedPostgresContainer`. Assertions must state the divergence precisely:
  (1) saving two accounts whose emails differ only by case succeeds at the repository level, proving PostgreSQL's unique index is case-sensitive and that the fake's case-insensitive matching was accidental;
  (2) `AccountService.register` rejects the second registration because it normalises email (trim + lowercase) before saving, so case-insensitive uniqueness is an explicit application rule rather than a fake's accident;
  (3) `repository.findByEmail` is case-sensitive, so the normalised value is what callers must query with;
  (4) `findAll(Sort.by(ASC, "email"))` returns email-sorted rows, proving ordering must be requested explicitly instead of assumed from insertion order;
  (5) `balance` reflects persisted state across a flush/clear boundary.
  Observe failure (module, container helper and repository absent).
- [ ] Implement `SharedPostgresContainer` (started singleton, `postgres:17-alpine`), the entity/repository/service, and `TestingJpaConfiguration`; run `./gradlew :modules:12-testing:integrationTest` green.
- [ ] Broken target: `InMemoryAccountRepository` with a `Map`, case-insensitive email matching, insertion-order results and no constraint enforcement, plus an `AccountServiceTest` that asserts those fake-only semantics. Issues: Testing issue (substitute diverges from production semantics), Database issue (constraint and collation assumptions unverified), Reliability issue (defects reach production undetected), Maintainability issue (fake duplicates repository logic).
- [ ] Document the H2 manifestation of the same failure in `SOLUTION.md` and note that H2 stays banned in this repository.
- [ ] Register issues in `docs/issues/reliability.md`, `docs/issues/database.md`, `docs/issues/maintainability.md`.
- [ ] Run `build` (no Docker) and `integrationTest` (Docker); commit: `Test persistence against real PostgreSQL`.

---

### Task 8: Review exercise — order-dependent suite

**Files:**
- Create: `broken-examples/order-dependent-test-suite/{SequenceAllocator.java,SequenceAllocatorTest.java,REVIEW.md,SOLUTION.md}`
- Create: `src/main/java/lab/testing/sequence/SequenceAllocator.java`
- Create: `src/test/java/lab/testing/sequence/SequenceAllocatorTest.java`

**Interfaces:**
```java
public final class SequenceAllocator { public SequenceAllocator(long start); public long next(); public long current(); public void reset(); }
```

- [ ] RED: write `SequenceAllocatorTest` with independent tests, each constructing its own allocator, asserting `next()` progression, `reset()` and overflow-adjacent values; observe failure.
- [ ] Implement `SequenceAllocator` with instance state; run green, then run the class twice in the same JVM (via `--tests` repetition) to prove independence.
- [ ] Broken target: static counter plus tests that assert absolute values only correct when executed in declaration order, and an `@TestMethodOrder` annotation hiding it. Issues: Testing issue (order dependency), Testing issue (shared static state), Maintainability issue (suite cannot run in parallel).
- [ ] Register issues in `docs/issues/reliability.md` and `docs/issues/maintainability.md`; commit: `Make sequence tests independent of execution order`.

---

### Task 9: Dedicated question example classes

**Files:**
- Create: `src/examples/java/lab/testing/questions/Q01…Q23*.java` (23 classes)

- [ ] One `final` class per question with a private constructor and a `main` method that prints or computes the concept, each demonstrating runtime results in trailing comments (e.g. `// true`, `// "expected"`).
- [ ] Coverage maps to Task 11's questions: pyramid and test levels; behaviour vs implementation assertions; test doubles; Mockito strictness; JUnit 5 lifecycle and extensions; parameterized tests; slices; Testcontainers lifecycle and `@ServiceConnection`; Awaitility polling; WireMock matching and verification; test data builders; ArchUnit rules; determinism and flakiness; mutation testing; contract testing; CI parallelism; incident diagnostics.
- [ ] No external I/O; no Docker; no sleeps.
- [ ] Run `./gradlew :modules:12-testing:compileExamples`; commit: `Add compiled testing question examples`.

---

### Task 10: Write testing concepts, internals and overview

**Files:** `docs/topics/testing/{index,concepts,internals}.md`

- [ ] `concepts.md` covers the full curriculum: testing pyramid and levels; unit/component/contract/E2E trade-offs; JUnit 5 (parameterized, nested, extensions); Mockito strictness and argument captors; AssertJ; slices (`@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`); Testcontainers with `@ServiceConnection` and singleton containers; WireMock; Awaitility; database and messaging integration tests; determinism and flaky tests; fixtures and builders; ArchUnit; mutation testing concepts; consumer-driven contracts.
- [ ] `internals.md` traces how the machinery works: JUnit Platform engines and extension callbacks, Mockito bytecode proxies and strict-stub detection, Spring `TestContext` caching and context key, slice auto-configuration, `@DynamicPropertySource`/`@ServiceConnection` property resolution, Testcontainers lifecycle and Ryuk, WireMock stub matching internals, Awaitility polling loop and condition evaluation, ArchUnit's bytecode import and rule evaluation. Add Mermaid diagrams.
- [ ] `index.md`: why this matters, how to study the module, links to the six exercises, and a progress table.
- [ ] Embed the six exercises and example classes via checked snippets; every page ends with `## Related`.
- [ ] Run `.venv/bin/mkdocs build --strict`; commit: `Document testing concepts and internals`.

---

### Task 11: Write hidden testing interview answers

**Files:** `docs/topics/testing/questions.md`, `docs/questions/testing.md`

- [ ] 8 Basic: testing pyramid; unit vs integration vs component vs E2E; behaviour vs implementation assertions; why `Thread.sleep` is banned; test doubles taxonomy; Mockito strict stubs; `@SpringBootTest` vs slices; what makes a test deterministic.
- [ ] 8 Intermediate: Mockito captors and strictness; JUnit 5 lifecycle and extensions; parameterized tests; slice contents; Testcontainers lifecycle and `@ServiceConnection`; Awaitility mechanics; WireMock matching and verification; builders and fixture isolation; ArchUnit rules.
- [ ] 5 Senior: triaging a flaky suite; balancing mocking against integration and contract tests; Testcontainers in CI at scale; mutation testing to expose weak assertions; consumer-driven contracts in a microservice fleet.
- [ ] 2 Scenario, symptom-first: suite passes locally but fails intermittently in CI; integration tests pass while production fails after a database upgrade.
- [ ] Every question: visible heading only, one collapsed full answer, nested collapsed snippet pointing at its dedicated `Qnn…` class; links to concept page, `code-review.md` anchor and `solutions.md` anchor.
- [ ] Surface via `docs/questions/testing.md` snippet includes with docs-root-relative links; verify 23/23/23 counts and strict build; commit: `Add hidden testing interview answers`.

---

### Task 12: Complete review, solution, tests, production and exercise pages

**Files:** `docs/topics/testing/{code-review,solutions,tests,production,exercises}.md`

- [ ] `code-review.md`: one section per exercise — context, embedded clean target, review dimensions from `REVIEW.md`, and a collapsed `??? warning "Reveal issues"` containing problem, why it happens, production impact, how to detect it, fix and trade-off.
- [ ] `solutions.md`: issue-to-fix matrix for all six exercises, before/after checked snippets, and the trade-offs of each correct implementation.
- [ ] `tests.md`: separates deterministic unit/slice tests from Docker-backed integration tests, lists exact commands (`./gradlew :modules:12-testing:test`, `integrationTest`, `compileExamples`), and documents the `test-support` container contract and CI implications.
- [ ] `production.md`: problems and diagnostics — flaky-test triage with repeat runs and thread dumps, mock-only suites hiding contract drift, sleep-based waits, fixture leakage, embedded-substitute divergence, container startup cost and Ryuk, plus a production checklist.
- [ ] `exercises.md`: ≥2 exercises with collapsed complete solutions (write a deterministic Awaitility-based test for a given async component; convert a mock-heavy test into a WireMock contract test).
- [ ] Reconcile every `SOLUTION.md` issue with exactly one catalogue file and a back-link; every page ends with `## Related`; run strict docs build; commit: `Complete the testing learning loop`.

---

### Task 13: Final verification and progress completion

**Files:** `docs/progress.md`, `README.md`, `scripts/verify-all.sh` (only if a check is missing)

- [ ] Run `./gradlew spotlessApply build integrationTest compileExamples compileBrokenExamples`.
- [ ] Run `.venv/bin/mkdocs build --strict` and `scripts/verify-all.sh`.
- [ ] Hoist the Docker API-version workaround (`systemProperty("api.version", "1.40")`) from `modules/12-testing/build.gradle.kts` into the `integrationTest` task registered in `build-logic/src/main/kotlin/lab.java-conventions.gradle.kts`, then remove the module-level duplication and confirm `:modules:12-testing:integrationTest` still passes.
- [ ] Structural checks: exactly six Module 12 broken examples; each has `REVIEW.md`, `SOLUTION.md` and a clean source; no ` issue:` in clean targets; each recorded correct package exists with tests; 23 testing questions with matching answer/example counts; no `Thread.sleep` in `src/test` or `src/integrationTest`; no `com.h2database` anywhere; `build` succeeds without Docker.
- [ ] Invoke `/reviewing-lab-change` and `/verifying-module 12-testing`; fix every Blocking/Should-fix finding and rerun all checks.
- [ ] Set the Module 12 progress cells to ✅ and update the README study instructions if they list module status.
- [ ] Commit: `Complete the testing module`.
