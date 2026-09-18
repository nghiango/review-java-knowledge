# Bootstrap and Core Java Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reproducible Java 21 / Gradle / MkDocs foundation and complete baseline Module 01 Core Java with four realistic review exercises, correct implementations, tests, questions and production guidance.

**Architecture:** The root is a Gradle multi-project aggregator with reusable convention plugins in `build-logic/`; only completed modules are included. `docs/` is canonical prose and includes source from `modules/` via checked snippets. Module 01 is JDK-only production code with four independent packages, while broken examples compile through an opt-in source set that never participates in `build`.

**Tech Stack:** Java 21; Gradle 9.7.1 Kotlin DSL; Spring Boot 3.5.16 pinned for future modules; JUnit Jupiter 5.14.4; AssertJ 3.27.7; Spotless 8.10.2; Error Prone plugin 5.1.1 / core 2.50.0; MkDocs Material 9.7.7.

**Spec:** `docs/superpowers/specs/2026-09-18-bootstrap-core-java-design.md`

## Global Constraints

- Baseline is Java 21 and Spring Boot 3.5.16; versions are pinned in `gradle/libs.versions.toml`.
- No Lombok, H2, field injection, floating dependency versions or Docker dependency in `./gradlew build`.
- Production code is test-first: observe the expected failure before writing each implementation.
- Review targets contain no categorized answer comments; answers exist only in `SOLUTION.md` and collapsed documentation.
- `./gradlew build` excludes `broken-examples`; `compileBrokenExamples` compiles them explicitly.
- Docs embed source with `pymdownx.snippets`; never paste production/review source into topic pages.
- Module 01 must contain four broken examples and at least 8 / 8 / 5 / 2 basic/intermediate/senior/scenario questions.
- Do not start tracks or modules 02–30 in this increment.

---

## File map

### Build and scripts

- `settings.gradle.kts` — plugin management and includes `build-logic` + `modules:01-core-java`.
- `build.gradle.kts` — root lifecycle aggregators (`integrationTest`, `compileBrokenExamples`); track aliases are deferred until tracks exist.
- `gradle.properties` — caching, parallel execution, UTF-8, warning mode.
- `gradle/libs.versions.toml` — all pinned dependency/plugin versions.
- `build-logic/settings.gradle.kts`, `build-logic/build.gradle.kts` — convention-plugin build.
- `build-logic/src/main/kotlin/lab.java-conventions.gradle.kts` — Java/test/integration/broken source sets and quality plugins.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*` — Gradle 9.7.1 wrapper.
- `scripts/verify-all.sh`, `scripts/run-module.sh` — reproducible verification and module runner.

### Module 01 production interfaces

- `mutablemapkey/CustomerKey.java` — `record CustomerKey(String tenantId, String customerId)`.
- `mutablemapkey/CustomerSnapshot.java` — immutable customer snapshot with `List.copyOf(tags)`.
- `optionalerrors/CustomerProfile.java` — immutable profile record.
- `optionalerrors/CustomerProfileRepository.java` — `Optional<CustomerProfile> findByEmail(String email)`.
- `optionalerrors/CustomerNotFoundException.java` — domain absence with requested email.
- `optionalerrors/CustomerProfileService.java` — `CustomerProfile requireProfile(String email)`.
- `resourceprocessing/CustomerCsvSource.java` — `BufferedReader open() throws IOException`.
- `resourceprocessing/ImportedCustomer.java` — validated row record.
- `resourceprocessing/ImportFailure.java` — line number, source row, reason.
- `resourceprocessing/ImportResult.java` — immutable accepted/failure lists.
- `resourceprocessing/CustomerCsvImporter.java` — `ImportResult importFrom(CustomerCsvSource source) throws IOException`.
- `streamprocessing/Order.java`, `OrderPrice.java`, `PricedOrder.java` — immutable report values.
- `streamprocessing/PriceClient.java` — `OrderPrice lookup(String orderId)`.
- `streamprocessing/OrderPricingException.java` — contextual failure.
- `streamprocessing/OrderReportService.java` — sequential `generate` and executor-backed `generateConcurrently`.

### Learning artefacts

- Four directories under `modules/01-core-java/broken-examples/` with `REVIEW.md`, clean Java and `SOLUTION.md`.
- Nine topic pages under `docs/topics/core-java/`.
- Eleven fixed catalogue files under `docs/issues/`, Java question navigation, site indexes and MkDocs configuration.

---

### Task 1: Bootstrap the Gradle multi-project build

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/lab.java-conventions.gradle.kts`
- Create: `modules/01-core-java/build.gradle.kts`
- Create: Gradle wrapper files

**Interfaces:**
- Produces Gradle project `:modules:01-core-java` and tasks `test`, `integrationTest`, `compileBrokenExamples`.
- `brokenExamples` consumes `modules/01-core-java/broken-examples/**/*.java` with the main compile classpath but is absent from `build`/`check` dependencies.

- [ ] **Step 1: Pin the build inputs**

Create `gradle/libs.versions.toml`:

```toml
[versions]
spring-boot = "3.5.16"
foojay-resolver = "1.0.0"
junit = "5.14.4"
assertj = "3.27.7"
spotless = "8.10.2"
errorprone-plugin = "5.1.1"
errorprone-core = "2.50.0"
google-java-format = "1.28.0"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
errorprone-core = { module = "com.google.errorprone:error_prone_core", version.ref = "errorprone-core" }

[plugins]
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
errorprone = { id = "net.ltgt.errorprone", version.ref = "errorprone-plugin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

- [ ] **Step 2: Define project inclusion and repositories**

Create `settings.gradle.kts` with `rootProject.name = "senior-java-spring-interview-lab"`,
apply `org.gradle.toolchains.foojay-resolver-convention` 1.0.0, `includeBuild("build-logic")`,
Maven Central repositories and `include("modules:01-core-java")`. Create
`build-logic/settings.gradle.kts` importing `../gradle/libs.versions.toml` as catalog
`libs`, and `build-logic/build.gradle.kts` with `kotlin-dsl`, Spotless and Error Prone plugin
artifacts on its implementation classpath.

- [ ] **Step 3: Define the convention plugin**

Implement `lab.java-conventions.gradle.kts` with:

```kotlin
plugins {
    java
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

dependencies {
    "testImplementation"(libs.junit.jupiter)
    "testImplementation"(libs.assertj.core)
    "errorprone"(libs.errorprone.core)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
```

Add `integrationTest` via `sourceSets.create("integrationTest")`, extend its implementation/runtime
configurations from test configurations, register `Test("integrationTest")`, and make it run after
`test` without attaching it to `check`. Add `brokenExamples` rooted at `broken-examples`, extend
from main implementation/compile-only configurations, and register an aggregator named
`compileBrokenExamples` depending on that source set's compile task. Configure UTF-8, `-parameters`,
Spotless Google Java Format AOSP 1.28.0 and Error Prone for JavaCompile tasks.

- [ ] **Step 4: Add module and root lifecycle files**

`modules/01-core-java/build.gradle.kts`:

```kotlin
plugins { id("lab.java-conventions") }
```

Root `build.gradle.kts` registers `integrationTest` and `compileBrokenExamples` tasks depending on
matching tasks in subprojects after evaluation. `gradle.properties` enables caching, parallelism,
configuration cache, UTF-8 and `org.gradle.warning.mode=all`.

- [ ] **Step 5: Generate and validate the wrapper**

Run `gradle wrapper --gradle-version 9.7.1 --distribution-type bin` (use an installed Gradle or a
temporary downloaded distribution if no `gradle` command exists), then:

```bash
./gradlew projects
./gradlew :modules:01-core-java:tasks --all
```

Expected: Module 01 listed; `test`, `integrationTest`, `compileBrokenExamples` listed; no code yet.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle build-logic modules/01-core-java/build.gradle.kts
git commit -m "Bootstrap the Java learning build"
```

---

### Task 2: Bootstrap the strict MkDocs portal

**Files:**
- Create: `mkdocs.yml`, `requirements-docs.txt`
- Create: `docs/index.md`, `docs/roadmap.md`, `docs/interview-checklist.md`, `docs/progress.md`, `docs/architecture.md`
- Create: `docs/issues/{index,security,performance,concurrency,transaction,data-consistency,database,messaging,resilience,reliability,observability,maintainability}.md`
- Create: `docs/questions/{index,java}.md`
- Create: `docs/scenarios/index.md`
- Create: `docs/reference/index.md`

**Interfaces:**
- Produces a strict site with Start, Topics, Issues, Scenarios, Questions, Reference and Contributing navigation.
- Future modules appear only in roadmap/progress, not as dead navigation links.

- [ ] **Step 1: Pin documentation dependencies**

Create:

```text
mkdocs==1.6.1
mkdocs-material==9.7.7
pymdown-extensions==10.16.1
```

- [ ] **Step 2: Configure MkDocs**

Use the baseline from `docs/spec/documentation-conventions.md` §13 with `strict: true`,
`docs_dir: docs`, Mermaid custom fence, snippet `base_path: [".", "docs"]`, `check_paths: true`,
and navigation only to files created in this plan. Include all specs and the approved design/plan
under Contributing.

- [ ] **Step 3: Create learner navigation pages**

`docs/index.md` contains the Start Here dashboard and eight-step study loop.
`docs/roadmap.md` lists bootstrap → modules 01–30 → tracks.
`docs/progress.md` contains all baseline rows with Module 01 🟨 and all other columns ⬜, plus empty
track sections.
`docs/interview-checklist.md` starts with the Module 01 checklist from its curriculum.
`docs/architecture.md` explains build boundaries, canonical prose/source split and two-view rule.

- [ ] **Step 4: Create the fixed catalogue/navigation skeletons**

Each catalogue file has a title, category purpose, empty `## Entries` section and `## Related`.
The catalogue index maps all 21 category strings to the fixed files using the table in
`issue-format.md` §6. Create scenario, question and reference indexes without links to nonexistent
content.

- [ ] **Step 5: Verify docs before topic content**

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements-docs.txt
.venv/bin/mkdocs build --strict
```

Expected: success with the spec, scaffold and no missing links.

- [ ] **Step 6: Commit**

```bash
git add mkdocs.yml requirements-docs.txt docs/index.md docs/roadmap.md docs/interview-checklist.md docs/progress.md docs/architecture.md docs/issues docs/questions docs/scenarios docs/reference
git commit -m "Create the strict learning portal"
```

---

### Task 3: Implement immutable identity and snapshots with TDD

**Files:**
- Create: `modules/01-core-java/src/test/java/lab/corejava/mutablemapkey/CustomerKeyTest.java`
- Create: `modules/01-core-java/src/test/java/lab/corejava/mutablemapkey/CustomerSnapshotTest.java`
- Create: `modules/01-core-java/src/main/java/lab/corejava/mutablemapkey/CustomerKey.java`
- Create: `modules/01-core-java/src/main/java/lab/corejava/mutablemapkey/CustomerSnapshot.java`

**Interfaces:**
- `CustomerKey(String tenantId, String customerId)` rejects blank components.
- `CustomerSnapshot(CustomerKey key, String displayName, List<String> tags)` uses `List.copyOf` and rejects nulls.

- [ ] **Step 1: Write failing equality/validation tests**

```java
@Test
void equalValues_remainUsableAsMapKeys() {
    var first = new CustomerKey("tenant-a", "customer-42");
    var sameIdentity = new CustomerKey("tenant-a", "customer-42");
    var cache = new HashMap<CustomerKey, String>();
    cache.put(first, "LOW");
    assertThat(cache.get(sameIdentity)).isEqualTo("LOW");
}

@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = " ")
void blankTenantId_isRejected(String tenantId) {
    assertThatThrownBy(() -> new CustomerKey(tenantId, "customer-42"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tenantId");
}
```

Add symmetric validation for customerId.

- [ ] **Step 2: Run RED**

```bash
./gradlew :modules:01-core-java:test --tests '*CustomerKeyTest'
```

Expected: compilation failure because `CustomerKey` does not exist.

- [ ] **Step 3: Implement `CustomerKey`**

```java
public record CustomerKey(String tenantId, String customerId) {
    public CustomerKey {
        tenantId = requireText(tenantId, "tenantId");
        customerId = requireText(customerId, "customerId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
```

- [ ] **Step 4: Run GREEN for key tests**

Expected: all `CustomerKeyTest` tests pass.

- [ ] **Step 5: Write failing snapshot immutability test**

Construct from mutable tags, mutate the original, assert snapshot unchanged; assert `tags().add`
throws `UnsupportedOperationException`; test null displayName/tags rejection.

- [ ] **Step 6: Run RED, implement snapshot, run GREEN**

Implementation signature:

```java
public record CustomerSnapshot(CustomerKey key, String displayName, List<String> tags) {
    public CustomerSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        tags = List.copyOf(tags);
    }
}
```

Run both test classes and `spotlessApply`, then run them again.

- [ ] **Step 7: Commit**

```bash
git add modules/01-core-java/src
git commit -m "Model stable customer identity"
```

---

### Task 4: Add the mutable-map-key review exercise

**Files:**
- Create: `modules/01-core-java/README.md`
- Create: `modules/01-core-java/broken-examples/mutable-map-key/{REVIEW.md,CustomerKey.java,CustomerRiskCache.java,SOLUTION.md}`
- Modify: `docs/issues/data-consistency.md`, `docs/issues/maintainability.md`

**Interfaces:**
- Broken package is `lab.corejava.broken.mutablemapkey`.
- `SOLUTION.md` records correct package `lab.corejava.mutablemapkey`.

- [ ] **Step 1: Write clean review targets**

Create a mutable `CustomerKey` whose `equals` compares tenant + customer but `hashCode` uses
customer only, setters permit mutation, and tags are returned directly; create a cache that stores
by key then mutates it during customer refresh. Add no `issue:` comments.

- [ ] **Step 2: Write `REVIEW.md`**

Ask the reviewer to inspect equality contracts, HashMap invariants, mutability, encapsulation,
subclass semantics and concurrency; instruct them to record findings before opening the solution.

- [ ] **Step 3: Write `SOLUTION.md`**

Annotate exactly four findings: Data consistency (mutated key becomes unreachable), Design
(equality/hash mismatch), Maintainability (mutable internal tags escape), Design (non-final key
with value equality). Include the issue table and full Problem/Why/Impact/Broken/Correct/Why/
Trade-offs/Detect/Follow-up/Related blocks.

- [ ] **Step 4: Register catalogue entries and compile**

Add entries and backlinks to the two fixed catalogue files. Run:

```bash
./gradlew compileBrokenExamples
```

Expected: success, and `./gradlew build --dry-run` must not list `compileBrokenExamples`.

- [ ] **Step 5: Commit**

```bash
git add modules/01-core-java/README.md modules/01-core-java/broken-examples/mutable-map-key docs/issues/data-consistency.md docs/issues/maintainability.md
git commit -m "Add the mutable map key review exercise"
```

---

### Task 5: Implement Optional and exception boundaries with TDD, then add its review exercise

**Files:**
- Create: production and tests under `optionalerrors/`
- Create: `broken-examples/optional-exception-misuse/{REVIEW.md,CustomerProfile.java,CustomerProfileService.java,SOLUTION.md}`
- Modify: `docs/issues/reliability.md`, `docs/issues/maintainability.md`

**Interfaces:**

```java
public record CustomerProfile(String customerId, String email, String displayName) {}
public interface CustomerProfileRepository { Optional<CustomerProfile> findByEmail(String email); }
public final class CustomerNotFoundException extends RuntimeException { public String email(); }
public final class CustomerProfileService {
    public CustomerProfileService(CustomerProfileRepository repository);
    public CustomerProfile requireProfile(String email);
}
```

- [ ] **Step 1: Write failing service tests**

Use a hand-written fake repository, not Mockito. Tests: found returns profile; empty throws
`CustomerNotFoundException` containing email; blank/null rejected before repository invocation;
repository `IllegalStateException` propagates unchanged.

- [ ] **Step 2: Run RED**

Expected: production types missing.

- [ ] **Step 3: Implement minimal records, repository, exception and service**

Normalize email with `strip().toLowerCase(Locale.ROOT)`, call repository once, use
`orElseThrow(() -> new CustomerNotFoundException(normalizedEmail))`; do not catch infrastructure
exceptions.

- [ ] **Step 4: Run GREEN and full Module 01 tests**

```bash
./gradlew :modules:01-core-java:test --tests '*optionalerrors*'
./gradlew :modules:01-core-java:test
```

- [ ] **Step 5: Write the two-view broken exercise**

Broken source contains an `Optional<CustomerProfile>` field and Optional input parameter,
`repository.findByEmail(...).get()`, catches `Exception`, and returns null. `SOLUTION.md` has four
matching issues: API design, Design, Reliability, Maintainability. Record correct package
`lab.corejava.optionalerrors`.

- [ ] **Step 6: Register, compile and commit**

Run `compileBrokenExamples`; add catalogue backlinks; commit production, tests, exercise and
catalogue entries as `Model profile absence explicitly`.

---

### Task 6: Implement resource-safe CSV processing with TDD, then add its review exercise

**Files:**
- Create: production and tests under `resourceprocessing/`
- Create: `broken-examples/resource-collection-mutation/{REVIEW.md,CustomerCsvImporter.java,SOLUTION.md}`
- Modify: `docs/issues/performance.md`, `docs/issues/reliability.md`, `docs/issues/maintainability.md`

**Interfaces:**

```java
@FunctionalInterface
public interface CustomerCsvSource { BufferedReader open() throws IOException; }
public record ImportedCustomer(String customerId, String email) {}
public record ImportFailure(int lineNumber, String sourceRow, String reason) {}
public record ImportResult(List<ImportedCustomer> accepted, List<ImportFailure> failures) {}
public final class CustomerCsvImporter { public ImportResult importFrom(CustomerCsvSource source) throws IOException; }
```

- [ ] **Step 1: Write RED tests**

Use a `CloseTrackingReader extends StringReader` overriding `close()`. Tests assert: reader closes
after success; closes after `IOException` while reading; malformed and blank-email rows become
`ImportFailure` with line/context; caller receives immutable lists; valid rows preserve order.

- [ ] **Step 2: Implement minimal importer and values**

Open with `try (BufferedReader reader = source.open())`; read one line at a time with a 1-based
counter; parse exactly two comma-separated fields; append to local accepted/failure lists; return
`new ImportResult(List.copyOf(...), List.copyOf(...))`. Do not mutate caller-owned collections.

- [ ] **Step 3: Run GREEN**

Run resource tests, then all Module 01 tests.

- [ ] **Step 4: Write two-view exercise**

Broken importer opens a reader without try-with-resources, loads rows into caller list, removes
inside enhanced `for`, and leaves partial mutation on parse failure. Annotate four issues:
Resource leak, Concurrency (fail-fast structural modification), Data consistency (partial caller
mutation), Design (parsing + mutation coupling). Record correct package.

- [ ] **Step 5: Register, compile and commit**

Run `compileBrokenExamples`; add fixed-catalogue entries/backlinks; commit as
`Make CSV imports resource safe`.

---

### Task 7: Implement deterministic stream reporting with TDD, then add its review exercise

**Files:**
- Create: production and tests under `streamprocessing/`
- Create: `broken-examples/stream-parallel-side-effects/{REVIEW.md,OrderReportService.java,PriceClient.java,SOLUTION.md}`
- Modify: `docs/issues/concurrency.md`, `docs/issues/performance.md`, `docs/issues/observability.md`

**Interfaces:**

```java
public record Order(String orderId, String customerId) {}
public record OrderPrice(BigDecimal amount, Currency currency) {}
public record PricedOrder(Order order, OrderPrice price) {}
public interface PriceClient { OrderPrice lookup(String orderId); }
public final class OrderPricingException extends RuntimeException { public String orderId(); }
public final class OrderReportService {
    public OrderReportService(PriceClient client, Executor executor);
    public List<PricedOrder> generate(List<Order> orders);
    public List<PricedOrder> generateConcurrently(List<Order> orders);
}
```

- [ ] **Step 1: Write RED sequential tests**

Assert `generate` preserves encounter order, returns immutable output, does not mutate input and
wraps lookup failure in `OrderPricingException` carrying orderId and original cause.

- [ ] **Step 2: Implement sequential pipeline and run GREEN**

Use `orders.stream().map(this::price).toList()` and contextual exception wrapping in `price`.

- [ ] **Step 3: Write RED bounded-concurrency test**

Pass a fixed pool of size 2. A fake client increments active/max-active atomics, blocks on a
`CountDownLatch`, and signals when two calls enter. Invoke `generateConcurrently` on another test
executor; await the entered latch with a bounded JUnit timeout, assert max active is 2, release,
and assert input order preserved. No sleeps.

- [ ] **Step 4: Implement concurrent method and run GREEN**

Create one `CompletableFuture.supplyAsync(() -> price(order), executor)` per order and join futures
in input order. The executor is caller-owned and is never shut down by the service.

- [ ] **Step 5: Write two-view exercise**

Broken report uses `parallelStream()`, adds to shared `ArrayList`, performs blocking client lookup
in common pool, assumes result order and rethrows context-free RuntimeException. Annotate:
Concurrency, Performance, Data consistency, Observability. Record correct package.

- [ ] **Step 6: Register, verify and commit**

Run all tests and `compileBrokenExamples`; add catalogue entries; commit as
`Bound concurrent order pricing`.

---

### Task 8: Write Core Java concepts and internals

**Files:**
- Create: `docs/topics/core-java/{index,concepts,internals}.md`
- Modify: `mkdocs.yml`, `docs/interview-checklist.md`

**Interfaces:**
- Canonical concept anchors are consumed by questions and `## Related` links.

- [ ] **Step 1: Write `index.md`**

Use required sections: Why this matters, Core Concepts, How it works internally, Common Interview
Questions, Common Production Problems, Broken Examples, Correct Implementations, Trade-offs,
Production Checklist, Senior-Level Questions, Exercises, Further Experiments. Link all topic pages
and the four exercises.

- [ ] **Step 2: Write `concepts.md`**

Cover every Module 01 curriculum item in grouped sections: object model/SOLID; equality/order;
immutability/Java 21 types; String/pass-by-value/modifiers; exceptions/resources/Optional;
collections; generics; streams; `var`/`java.time`. Each group includes when to use, when not,
common failure and trade-off. No source copies.

- [ ] **Step 3: Write `internals.md`**

Explain HashMap hash spreading, buckets, resize, collision list → red-black tree threshold (capacity
conditions included), equality dispatch, type erasure/bridge methods, stream sink pipeline/lazy
execution/stateful barriers, String compact-string storage/pool. Add Mermaid diagrams for HashMap
lookup and stream evaluation.

- [ ] **Step 4: Update nav and checklist; verify**

Add Core Java pages under Topics. Expand checklist to cover equality, HashMap, generics/PECS,
exceptions/resources, stream laziness/stateful operations, parallel-stream trade-offs and Java 21
features. Run `mkdocs build --strict`.

- [ ] **Step 5: Commit**

Commit as `Document Core Java foundations`.

---

### Task 9: Write the complete interview question set

**Files:**
- Create: `docs/topics/core-java/questions.md`
- Modify: `docs/questions/java.md`, `mkdocs.yml`

**Interfaces:**
- Section markers expose Basic, Intermediate, Senior and Scenarios to `docs/questions/java.md` via snippets.

- [ ] **Step 1: Write eight basic questions**

Exact subjects: `==` vs `equals`; equals/hashCode contract; record purpose; checked vs unchecked;
Optional purpose; List vs Set vs Map; intermediate vs terminal stream operation; `? extends` vs
`? super`. Use Q + ≤5-line Short Answer + concept link.

- [ ] **Step 2: Write eight intermediate questions**

Exact subjects: HashMap lookup/collision; treeification conditions; mutable map keys; ArrayList
resize/insert costs; Comparator consistency with equals; type erasure; lazy/stateful stream
operations; try-with-resources suppressed exceptions. Use Short Answer + Internal Mechanism +
Common Mistake + links.

- [ ] **Step 3: Write five full senior questions**

Exact situations: mutable cache key causes intermittent misses; API equality changes break sets;
parallel stream performs remote pricing under load; Optional overuse crosses DTO/JPA boundaries;
CSV import leaks descriptors and partially mutates state. Each uses all seven answer sections and
links to its review/correct example.

- [ ] **Step 4: Write two symptom-first scenarios**

1. File descriptors climb, imports fail after hours, heap/CPU normal — diagnose and redesign.
2. Report endpoint returns missing/reordered rows, common ForkJoinPool saturated — diagnose and
redesign. Do not reveal the cause in question headings; use full answer format.

- [ ] **Step 5: Surface and verify**

Add named snippet markers around each level and include them from `docs/questions/java.md`. Add nav,
run a grep/count check proving 8/8/5/2, then `mkdocs build --strict`.

- [ ] **Step 6: Commit**

Commit as `Add senior Core Java interview questions`.

---

### Task 10: Complete code-review, solution, test, production and exercise pages

**Files:**
- Create: `docs/topics/core-java/{code-review,solutions,tests,production,exercises}.md`
- Modify: all catalogue files touched in Tasks 4–7, `mkdocs.yml`

**Interfaces:**
- Source blocks use checked `--8<--` paths.
- Every issue has bidirectional links topic ↔ catalogue ↔ exercise.

- [ ] **Step 1: Write `code-review.md`**

For each of four exercises: 2–4-line context; embed clean Java source; review dimensions; collapsed
`??? warning "Reveal issues"` containing all issue blocks; link to the matching solution anchor.
Never include annotated code above the reveal.

- [ ] **Step 2: Write `solutions.md`**

Four sections with Broken/Correct tabs using source snippets, issue-to-fix mapping table, Why it
works and Trade-offs. Explicitly explain: immutable key identity; absence vs failure; ownership of
closeable resources and immutable result; sequential purity vs explicit executor lifecycle.

- [ ] **Step 3: Write `tests.md`**

Map each test class to the behaviour it proves and list exact commands:
`:test`, per-package `--tests`, `compileBrokenExamples`. Explain why Module 01 has no integration
tests and why fake dependencies/latches are preferable to timing sleeps.

- [ ] **Step 4: Write `production.md`**

Incident sections: HashMap cache misses after mutation; file descriptor exhaustion; ForkJoinPool
starvation. Each includes symptoms, metrics/tools (`jcmd`, thread dump, `lsof`, pool counters), root
cause, fix, verification, prevention and checklist.

- [ ] **Step 5: Write `exercises.md`**

At least two: (1) design a case-insensitive immutable email key with correct equality; (2) replace
a side-effecting parallel collector while preserving order and failure context. Put complete
solutions in collapsed blocks and link relevant concepts/tests.

- [ ] **Step 6: Reconcile catalogue and nav**

Ensure every SOLUTION issue appears once in the correct fixed catalogue file and every entry's
"Appears in" links to its example and topic anchor. Add all pages to nav and run strict docs build.

- [ ] **Step 7: Commit**

Commit as `Complete the Core Java learning loop`.

---

### Task 11: Add repository scripts and honest progress state

**Files:**
- Create: `scripts/verify-all.sh`, `scripts/run-module.sh`
- Create: `.gitignore`
- Modify: `docs/progress.md`, `README.md`

**Interfaces:**
- `verify-all.sh` exits nonzero on any Gradle/docs/static-check failure.
- `run-module.sh core-java` explains that Module 01 is non-runnable; unknown/ambiguous slugs fail.

- [ ] **Step 1: Write `verify-all.sh`**

Use `set -euo pipefail`; run `./gradlew build`, `integrationTest`, `compileBrokenExamples`; run
static checks across baseline/tracks (if those directories exist); select `.venv/bin/mkdocs` when
available otherwise `mkdocs`; run `build --strict`. Exclude `SOLUTION.md` and `docs/spec` correctly
from answer/pasted-code checks.

- [ ] **Step 2: Write `run-module.sh`**

Resolve exactly one `modules/[0-9][0-9]-${slug}` directory. Check whether its Gradle project has a
`bootRun` task; if absent, print `Module '${slug}' is a library/demo module; run its tests with ...`
and exit 2. Never construct a project path from unchecked input.

- [ ] **Step 3: Add `.gitignore` and update README**

Ignore `.gradle/`, `build/`, `**/build/`, `.venv/`, `site/`, IDE metadata and OS files. Replace
README's bootstrap status with Module 01 completion only after verification succeeds.

- [ ] **Step 4: Set progress**

Keep Module 01 🟨 until Task 12 passes. Ensure all future modules remain ⬜ and track tables have no
false completion.

- [ ] **Step 5: Commit**

Commit as `Automate lab verification`.

---

### Task 12: Final verification and spec review

**Files:**
- Modify only files needed to fix discovered failures
- Modify: `docs/progress.md`, `README.md` after all checks pass

**Interfaces:**
- Produces evidence for all design acceptance criteria and Module 01 DoD.

- [ ] **Step 1: Format and run baseline checks**

```bash
./gradlew spotlessApply
./gradlew build
./gradlew integrationTest
./gradlew compileBrokenExamples
```

Expected: all successful; integrationTest may be `NO-SOURCE`; build task graph excludes broken compilation.

- [ ] **Step 2: Run structural/static checks**

Run the checks from `.devin/skills/verifying-module/SKILL.md`, plus scripts verifying exactly four
broken-example directories and required files, matching recorded correct packages, question counts,
fixed catalogue filenames and no categorized comments in clean targets. Fix every hit.

- [ ] **Step 3: Build documentation strictly**

```bash
.venv/bin/mkdocs build --strict
```

Expected: no warnings, missing snippet files or broken internal links.

- [ ] **Step 4: Run repository verifier**

```bash
scripts/verify-all.sh
```

Expected: exit 0.

- [ ] **Step 5: Review against specifications**

Invoke `/reviewing-lab-change`, then `/verifying-module 01-core-java`. Resolve every Blocking and
Should-fix finding. Confirm all code-module DoD lines and design acceptance criteria.

- [ ] **Step 6: Mark completion only now**

Set all genuinely complete Module 01 progress cells to ✅; update README status to say Module 01 is
complete and Module 02 is next. Re-run strict MkDocs build after this change.

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "Complete the initial Core Java lab"
```
