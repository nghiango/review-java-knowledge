# Bootstrap and Core Java Module Design

**Date:** 2026-09-18
**Status:** Approved in chat; awaiting review of this written specification
**Scope:** Repository bootstrap and complete baseline Module 01 (`core-java`)

## 1. Goal

Deliver the first independently useful increment of the Senior Java / Spring Interview Lab:
a reproducible Java 21 multi-project build, a strict MkDocs portal, repository verification
scripts, and a complete Module 01 learning loop from theory through production scenarios.

This increment does not create empty placeholders for modules 02–30 and does not implement the
Java 25 / Boot 4 or Kotlin tracks. Their specifications remain available for subsequent work.

## 2. Architecture

### 2.1 Baseline build

The root is a Gradle Kotlin DSL multi-project build. It aggregates only modules that contain real
content. Module 01 is the first included project.

- Java toolchain: 21
- Gradle wrapper: a stable version compatible with Java 21 and Spring Boot 3.5
- Spring Boot: exact, vetted 3.5.x patch pinned in `gradle/libs.versions.toml`
- Dependency versions and aliases: version catalog
- Shared build policy: convention plugins in `build-logic/`
- Module build files: apply convention plugins and declare only dependencies they use

The initial convention plugin provides:

- Java 21 toolchain and compiler settings
- JUnit Platform, JUnit Jupiter and AssertJ
- `src/integrationTest` source set and `integrationTest` task
- `brokenExamples` source set rooted at `broken-examples/`
- `compileBrokenExamples` lifecycle task, separate from `build` and `check`
- Spotless formatting with Google Java Format AOSP style
- Error Prone static analysis

`./gradlew build` never requires Docker and never compiles broken examples. Module 01 has no
integration tests because it has no infrastructure semantics; the integration-test mechanism is
created and verified as part of the convention plugin.

### 2.2 Documentation portal

MkDocs Material uses `docs/` as its canonical prose source and includes code from `modules/`
using `pymdownx.snippets` with path checking enabled. Dependencies in
`requirements-docs.txt` are pinned.

Initial documentation includes:

- Home dashboard and learning instructions
- Roadmap, interview checklist, progress table and repository architecture
- Complete Module 01 topic pages
- All 11 fixed issue-catalogue files
- Question-navigation index and Java question page
- Scenario and reference indexes needed by active navigation
- Specification documents under a Contributing section

Navigation does not contain dead links or placeholder pages for modules 02–30. The roadmap and
progress table list the full curriculum and mark future modules as not started.

### 2.3 Scripts

- `scripts/verify-all.sh` runs the applicable baseline Gradle checks and `mkdocs build --strict`.
  It detects whether Docker-backed integration tests exist before requiring Docker during this
  increment.
- `scripts/run-module.sh <slug>` resolves the canonical numbered module directory and invokes its
  `bootRun` task. For Module 01 it exits with a clear message because Core Java is a library/demo
  module without a Spring Boot application.

## 3. Module 01 boundaries

Module 01 is a code module at `modules/01-core-java`, package root `lab.corejava`.

The documentation covers the full curriculum entry:

- OOP, SOLID, composition vs inheritance
- Equality, hashing, comparison
- Immutability, records, enums, sealed types and Java 21 pattern matching
- String behaviour and pass-by-value
- Exceptions and resource management
- Optional
- Collections and HashMap internals
- Generics, wildcards, PECS and erasure
- Streams, laziness, stateful operations, collectors and parallel streams
- `var` and `java.time`

Runnable review exercises focus on four cohesive production failure families rather than one
exercise per API.

## 4. Broken and correct examples

Every example follows the two-view contract: clean Java review targets and `REVIEW.md` contain no
answers; `SOLUTION.md` contains the same code with exact categorized issue comments, a matching
issue table, complete issue explanations and links to the correct package and documentation.

### 4.1 `mutable-map-key`

Scenario: a customer-risk cache uses a mutable domain object as a HashMap key and has an equality
contract inconsistent with its business identity.

Broken concerns:

- Key mutation makes a stored entry unreachable
- `equals` and `hashCode` use different fields
- Mutable collections leak through accessors
- Equality semantics are undocumented and unsuitable for subclasses

Correct package: `lab.corejava.mutablemapkey`.

Correct design: immutable `CustomerKey` record with canonical validation; mutable customer data is
kept in a separate immutable snapshot. Tests prove lookup stability, equality contract and defensive
copying where applicable.

### 4.2 `optional-exception-misuse`

Scenario: a customer profile lookup uses `Optional` in fields and parameters, calls `get()`, catches
`Exception`, and converts distinct failures into absence.

Broken concerns:

- `Optional.get()` produces an unstable failure
- Optional used as field/parameter obscures contracts and complicates serialization
- Broad catch swallows operational failures
- `null` may be returned despite an Optional return type

Correct package: `lab.corejava.optionalerrors`.

Correct design: nullable input rejected at the boundary; repository absence is represented by
`Optional` only at the return boundary; domain `CustomerNotFoundException` remains distinct from
infrastructure failure. Tests cover found, absent, invalid input and propagated failure.

### 4.3 `resource-collection-mutation`

Scenario: CSV import manually opens a buffered reader and removes invalid rows from a list inside
an enhanced for-loop.

Broken concerns:

- Reader leaks on successful and exceptional paths
- Structural mutation triggers fail-fast iteration
- Partial mutation leaves caller-owned input in an indeterminate state
- Parsing and mutation are coupled, making failure recovery unclear

Correct package: `lab.corejava.resourceprocessing`.

Correct design: try-with-resources; parse into a new immutable result; collect validation failures
explicitly; do not mutate caller-owned input. Tests use a close-tracking reader and cover malformed
rows and immutable output.

### 4.4 `stream-parallel-side-effects`

Scenario: an order-report service uses a shared `ArrayList` in a parallel stream and performs a
blocking price lookup in the common ForkJoinPool.

Broken concerns:

- Non-thread-safe shared mutation causes lost/corrupt results
- Blocking I/O occupies common-pool workers
- Encounter order is assumed without enforcing it
- Exceptions lose order/customer context

Correct package: `lab.corejava.streamprocessing`.

Correct design: a pure sequential stream for ordinary reports and an explicit bounded executor
API for concurrent blocking lookups. Results carry input identity and preserve deterministic order;
failures retain context. Tests cover purity, ordering, failure context and concurrency bounds using
latches rather than sleeps.

## 5. Test-driven implementation

Correct production code is introduced only after a failing test demonstrates the desired behaviour.
Each correct package has focused tests in `src/test/java`. Red/green evidence is recorded in the
implementation session and final verification report; tests are not manufactured after code.

Broken examples are written independently as review artefacts, then compiled through
`compileBrokenExamples`; they are never imported into tests for correct code.

## 6. Documentation content

`docs/topics/core-java/` contains:

- `index.md`: why Core Java matters and study navigation
- `concepts.md`: the full curriculum, compact and cross-linked
- `internals.md`: HashMap, equality dispatch, type erasure, stream pipeline and string internals
- `questions.md`: 8 basic, 8 intermediate, 5 senior, 2 symptom-first scenarios
- `code-review.md`: four clean source snippets, prompts and collapsed issue reveals
- `solutions.md`: source-included correct implementations, before/after tabs, fix mapping and trade-offs
- `tests.md`: what each test proves and commands to run
- `production.md`: mutable-key cache misses, descriptor exhaustion, common-pool starvation and diagnostics
- `exercises.md`: at least two hands-on exercises with collapsed solutions

Each page ends with `## Related`. Global Java question navigation includes the topic question
sections by snippets instead of copying them.

## 7. Issue-catalogue mapping

Issues introduced by Module 01 are registered only in the fixed catalogue files:

- Equality/key mutation and leaked mutation → `maintainability.md` or `data-consistency.md`
- Resource leak and common-pool contention → `performance.md`
- Broad exception swallowing → `reliability.md`
- Parallel shared mutation → `concurrency.md`
- Missing failure context → `observability.md`

Every entry links back to the broken example and topic pages. Category strings match
`docs/spec/issue-format.md` exactly.

## 8. Error handling and production diagnostics

Correct examples distinguish invalid input, expected absence and operational failure. Exceptions
retain domain identifiers without logging secrets. The production page provides concrete diagnostic
signals: HashMap lookup anomalies, file-descriptor counts and stack traces, ForkJoinPool saturation,
thread dumps and failure-rate/error-context observations.

## 9. Verification and acceptance criteria

The increment is accepted when:

1. Gradle wrapper runs on JDK 21.
2. `./gradlew build` succeeds without Docker and runs Module 01 unit tests.
3. `./gradlew compileBrokenExamples` succeeds and is demonstrably not a dependency of `build`.
4. `./gradlew integrationTest` succeeds with `NO-SOURCE` or no Module 01 integration tests.
5. Static checks find no issue comments in review targets, no Lombok/H2 and no pasted source in docs.
6. All four broken examples have clean source, `REVIEW.md`, `SOLUTION.md`, correct package and tests.
7. Question counts are exactly or above 8 / 8 / 5 / 2.
8. Every Module 01 issue has a catalogue entry and backlink.
9. `mkdocs build --strict` succeeds with all active pages in navigation.
10. `docs/progress.md` marks only genuinely completed Module 01 columns as complete.
11. `scripts/verify-all.sh` succeeds for this repository state.

## 10. Out of scope

- Implementing modules 02–30
- Creating empty module directories
- Implementing Java 25 / Spring Boot 4 or Kotlin tracks
- Docker Compose services (none are needed by Module 01); the file is introduced when the first
  infrastructure-backed module requires it
- Publishing MkDocs or configuring hosted CI
- Coverage-percentage gates; tests are judged by behaviour and failure detection
