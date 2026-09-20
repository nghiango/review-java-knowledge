# Senior Java + Spring Interview Lab — Agent Operating Manual

This repository is a **Senior Java / Spring Backend Engineer interview + engineering laboratory**:
theory, interview Q&A, intentionally broken code for review practice, production-style correct
implementations, tests, production failure scenarios, and a MkDocs study portal.

This file is the short operating manual. The detailed specification lives in `docs/spec/`
and is **normative** — read the relevant spec before working on a module:

| Spec | Read it when |
|---|---|
| [docs/spec/module-conventions.md](docs/spec/module-conventions.md) | Creating or extending a module (layout, module types, question format, Definition of Done) |
| [docs/spec/issue-format.md](docs/spec/issue-format.md) | Writing broken examples, `SOLUTION.md`, or issue catalogue entries |
| [docs/spec/curriculum.md](docs/spec/curriculum.md) | Deciding what a module must cover and which broken examples it needs |
| [docs/spec/documentation-conventions.md](docs/spec/documentation-conventions.md) | Writing or changing anything under `docs/` or `mkdocs.yml` |
| [docs/spec/tracks.md](docs/spec/tracks.md) | Working under `tracks/` (Java 25 + Boot 4, Kotlin) or adding a new language/version track |

---

## 1. Stack

**Baseline** (`modules/`, the canonical reference implementation):

- Java **21**, Spring Boot **3.5.x** (pin the exact patch version in `gradle/libs.versions.toml`)
- Gradle Kotlin DSL, version catalog, convention plugins in `build-logic/`
- JUnit 5, AssertJ, Mockito, Awaitility, Testcontainers (PostgreSQL, Kafka, RabbitMQ, Redis), WireMock, ArchUnit
- PostgreSQL, Flyway, Spring Data JPA, Spring JDBC (`JdbcClient`), HikariCP
- Spring MVC, `RestClient`, WebClient/WebFlux (module 21 only)
- Spring Security, Spring Kafka, Spring AMQP, Spring Data Redis, Caffeine
- Resilience4j, Micrometer (Observation + Tracing with OpenTelemetry bridge), Actuator
- Jackson, Bean Validation, RFC 9457 Problem Details (`ProblemDetail`)
- Docker Compose for local infrastructure
- MkDocs + Material for MkDocs + PyMdown Extensions + Mermaid
- Optional (only where the curriculum says so): Spring Cloud, AWS SDK v2, LocalStack, Spring Modulith

**Not used in Java code:** Lombok, H2 for persistence behaviour, field injection (`@Autowired` on fields).

**Tracks** (`tracks/<id>/`, isolated Gradle included builds, each with its own toolchain, BOM and
version catalog; they teach the *delta* from the baseline, never duplicate it):

- `java25-boot4` — Java 25, Spring Boot 4.0.x / Spring Framework 7
- `kotlin` — Kotlin 2.x on the baseline JDK/Boot, coroutines, MockK
- New Java LTS / Boot major / language → new track, see `docs/spec/tracks.md`

## 2. Repository layout

```text
.
├── AGENTS.md                      ← this file
├── README.md
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/libs.versions.toml
├── build-logic/                   ← Gradle convention plugins
├── docker-compose.yml
├── mkdocs.yml / requirements-docs.txt
├── scripts/
│   ├── verify-all.sh              ← full verification (see §5)
│   └── run-module.sh <slug>       ← resolves NN from the slug, runs ./gradlew :modules:<NN-slug>:bootRun
├── docs/                          ← CANONICAL prose (MkDocs docs_dir)
│   ├── index.md, roadmap.md, interview-checklist.md, progress.md, architecture.md
│   ├── spec/                      ← project specification (normative)
│   ├── topics/<slug>/             ← one folder per baseline module
│   ├── tracks/<track-id>/         ← per-track delta docs (what's new, migration, <slug>/)
│   ├── issues/                    ← global issue catalogue
│   ├── scenarios/                 ← production incident walkthroughs
│   ├── questions/                 ← interview question navigation
│   └── reference/
├── modules/                       ← BASELINE: Java 21 + Boot 3.5.x
│   ├── test-support/              ← shared Testcontainers singletons, builders
│   └── NN-<slug>/                 ← 30 modules, see curriculum
│       ├── README.md              ← SHORT: what is here + link to docs/topics/<slug>/
│       ├── build.gradle.kts
│       ├── broken-examples/<example>/   ← review targets (never in the main build)
│       └── src/{main,test,integrationTest,examples}/java/lab/<topic>/...
└── tracks/<track-id>/             ← included builds: own settings, catalog, build-logic, test-support
    └── modules/NN-<slug>/         ← same slug as baseline for deltas; ≥ 40 for track-only modules
```

Topic **slugs** are defined once in `docs/spec/curriculum.md` and used identically for the module
directory (`modules/07-spring-transactions`), Gradle project (`:modules:07-spring-transactions`),
docs folder (`docs/topics/spring-transactions/`) and Java package root (`lab.springtransactions`).

`docs/` is the single source of truth for theory, questions, exercises and solutions.
`modules/` is the single source of truth for code. Docs embed code via `pymdownx.snippets`
(`--8<--`) — never paste copies of source files into Markdown.

## 3. Non-negotiable engineering rules

1. Broken code lives **only** under `modules/*/broken-examples/`. It is never compiled by
   `./gradlew build` or `check`. A dedicated `compileBrokenExamples` task compiles it on demand
   to prove it is real code, and `scripts/verify-all.sh` runs it.
2. Each broken example ships **two views**: the clean review target (`*.java`, `REVIEW.md`, no
   hints) and `SOLUTION.md` (the same code annotated with categorized inline issue comments plus the
   full issue list). Never put answer comments into the review-target sources.
3. Every issue comment uses the exact format `// <Category> issue: <explanation>` with a category
   from `docs/spec/issue-format.md`. The same category names are used in `SOLUTION.md`, topic docs
   and `docs/issues/`.
4. Every broken example has a corresponding correct implementation under `src/main/java` with tests,
   an explanation of why it fixes each issue, and its trade-offs.
5. Correct code compiles, runs, follows production practice: constructor injection, immutable
   data where practical, records for values, explicit timeouts on every external call, bounded
   retries, idempotent consumers, authorization as well as authentication, observability considered.
6. Tests: `src/test` = fast unit tests (no Docker). `src/integrationTest` = Testcontainers /
   `@SpringBootTest` (Docker required). `./gradlew build` runs unit tests only;
   `./gradlew integrationTest` runs the rest. Prefer PostgreSQL Testcontainers over H2. Do not hide
   infrastructure semantics behind mocks. No `Thread.sleep` in tests — use Awaitility.
7. Examples must resemble real pull-request code, never toy snippets. Always explain trade-offs;
   never present one solution as universally correct.
8. Documentation is concise but deep: Concept → Example → Failure → Fix → Trade-off → Interview
   question. No generic walls of text. Use Mermaid for flows. Cross-link heavily; one canonical page
   per concept.
9. Do not add dependencies outside §1 without asking. Do not modify security or build policy files
   to make a build pass.
10. Quality over quantity. Fully finish one module (see Definition of Done) before starting the next.
11. Tracks never duplicate baseline content. A track module covers only what differs on that
    stack and links to the baseline for everything else. Baseline must stay green regardless of
    track state; track builds are isolated included builds.

## 4. Implementation order

Bootstrap first: repo structure, `build-logic/`, version catalog, `test-support`, `docker-compose.yml`,
`mkdocs.yml`, docs skeleton, `docs/progress.md`.

Then implement modules **fully, in order**: 01 → 12 (Core Java through Testing). Prove the
convention on 07 Spring Transactions early — it is the anchor example used throughout the spec.
Continue 13 → 30 afterwards.

Tracks start once their baseline counterpart exists: a track module for slug `NN-<slug>` may only
be created after `modules/NN-<slug>` meets its Definition of Done. Track-only modules (≥ 40) and
the track's what's-new / migration docs can start any time.

## 5. Verification (run before claiming anything is done)

```bash
./gradlew build                     # baseline compile + unit tests, no Docker needed
./gradlew integrationTest           # baseline Testcontainers tests, Docker required
./gradlew compileBrokenExamples     # baseline broken examples still compile (never part of build)
./gradlew compileExamples           # question/demo examples under src/examples compile (runs under check)
./gradlew buildTracks               # all tracks (each: build + compileBrokenExamples)
./gradlew buildTrack-kotlin         # one track
mkdocs build --strict               # docs build, no broken links
scripts/verify-all.sh               # all of the above
```

A change is not complete if any of these fail, if documented code no longer matches the
implementation, or if `docs/progress.md` was not updated.

## 6. Agent workflow checklist

Project skills in `.agents/skills/` encode these workflows — invoke the matching one instead of
improvising: `/creating-module`, `/writing-broken-example`, `/writing-interview-questions`,
`/documenting-topic`, `/creating-track-module`, `/reviewing-lab-change`, `/verifying-module`.

When adding or changing a module:

- [ ] Read `docs/spec/curriculum.md` entry for the module and `docs/spec/module-conventions.md`
- [ ] Check the module type (code / doc) — doc modules have a reduced Definition of Done
- [ ] Create/extend `modules/NN-<slug>/` and `docs/topics/<slug>/` with the standard files
- [ ] Broken examples: clean sources + `REVIEW.md` + `SOLUTION.md`; register each issue in `docs/issues/`
- [ ] Correct implementation + unit tests (+ integration tests where infrastructure semantics matter)
- [ ] Questions at basic / intermediate / senior / scenario levels, linked to concept + code
- [ ] Add the module to `mkdocs.yml` nav, `docs/interview-checklist.md`, `docs/progress.md`
- [ ] Run §5 verification
- [ ] Commit with a message explaining *why*; never commit broken-example changes that silently alter the review target without updating `SOLUTION.md`

When working in a track, additionally:

- [ ] Read `docs/spec/tracks.md`; confirm the baseline module is done
- [ ] Scope to the delta; start each docs page with a **Delta** admonition linking to baseline
- [ ] Add ≥ 2 senior migration/upgrade questions
- [ ] Tag track-specific issues with `Track:` metadata in `docs/issues/`
- [ ] Update `docs/tracks/index.md` comparison matrix and the track section of `docs/progress.md`

## 7. Code style

- Packages: baseline `lab.<topic>.<example>` (e.g. `lab.springtransactions.externalcall`);
  tracks `lab.<segment>.<topic>.<example>` where `<segment>` is the track id without dashes
  (`lab.java25boot4...`, `lab.kotlin...`, see `docs/spec/tracks.md` §1); broken examples insert
  `.broken` before `<example>`. `<topic>` comes from the curriculum's Package root column.
- Kotlin track: idiomatic Kotlin (null safety, `val`, data classes for values, sealed hierarchies,
  coroutines over blocking), `kotlin-spring` + `kotlin-jpa` plugins, MockK, ktlint via Spotless.
  Java rules that do not apply to Kotlin (Lombok ban, records) are moot there.
- Records for DTOs/commands/value objects; explicit constructors for beans; `final` fields.
- Question examples: live in `src/examples/java/lab.<topic>.questions`, with exactly one dedicated compilable class per question, demonstrating runtime/evaluation results via trailing comments (e.g. `"a".equals("a") // true`).
- Exceptions: domain exceptions mapped centrally via `@RestControllerAdvice` to `ProblemDetail`.
- Comments in correct code explain **decisions and trade-offs**, not syntax. Do not add/remove
  comments unrelated to the task.
- Formatting via Spotless (Google Java Format, AOSP style); static analysis via Error Prone; both
  wired in `build-logic/` and run by `check`.
- Test names: `methodUnderTest_condition_expectedOutcome` or descriptive `@DisplayName`.
