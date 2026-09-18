# Tracks — Multiple Java / Spring Boot Versions and Kotlin

The repository is a living knowledge base, not a snapshot. A **track** is a self-contained variant
of the lab built on a different language/runtime/framework combination. Tracks let the developer
learn what changes between versions and languages while the baseline stays stable and reproducible.

## 1. Track registry

| Track id | Package segment | Language | Runtime | Framework | Role | Location |
|---|---|---|---|---|---|---|
| `baseline` | — (`lab.<topic>`) | Java 21 | JDK 21 | Spring Boot 3.5.x | Reference implementation of all 30 modules | `modules/` |
| `java25-boot4` | `java25boot4` | Java 25 | JDK 25 | Spring Boot 4.0.x | Newer-version delta track | `tracks/java25-boot4/` |
| `kotlin` | `kotlin` | Kotlin 2.x | JDK 21 (baseline JDK) | Spring Boot 3.5.x | Language delta track | `tracks/kotlin/` |
| `kotlin-boot4` (later) | `kotlinboot4` | Kotlin 2.x | JDK 25 | Spring Boot 4.0.x | Combined delta | `tracks/kotlin-boot4/` |

The **package segment** is the track id with dashes removed and is used verbatim as the second
package element: `lab.<segment>.<topic>[.broken].<example>`
(`lab.java25boot4.springtransactions.externalcall`, `lab.kotlin.concurrency.broken.runblocking`).

Adding a track = one row here, one directory under `tracks/`, one `docs/tracks/<id>/` folder,
one line in `settings.gradle.kts`. Versions are pinned per track in that track's own
`gradle/libs.versions.toml`.

## 2. Principles

1. **Baseline is canonical.** Every topic's theory, questions, broken examples and correct code
   exist first in `modules/` on the baseline stack. A track never replaces the baseline; it adds a
   delta.
2. **Tracks teach deltas, not duplicates.** A track module documents and implements *what is
   different*: new language features, changed framework APIs, removed/deprecated behaviour,
   migration pitfalls, new broken-example categories. Unchanged topics are linked to the baseline,
   not copied.
3. **Tracks are isolated builds.** Each track is a Gradle *included build* with its own toolchain,
   BOM, version catalog and convention plugins so that Java 25 / Boot 4 / Kotlin compiler settings
   never leak into the baseline. `./gradlew build` at the root builds the baseline only;
   `./gradlew buildTrack-<id>` (a root alias delegating into the included build) builds a track;
   `./gradlew buildTracks` builds all of them. Included builds are not addressable as
   `:tracks:<id>:...` project paths.
4. **Same conventions.** Track modules follow `module-conventions.md` and `issue-format.md`
   exactly: clean review targets + `SOLUTION.md`, correct implementation, tests, docs pages,
   issue catalogue registration. The only difference is scope (delta) and stack.
5. **Every track module answers the migration question.** "You are upgrading a Boot 3.5 service to
   Boot 4 — what breaks, what do you re-test, what do you gain?" is a senior interview question in
   its own right.

## 3. Layout

```text
.
├── modules/                          # baseline: Java 21 + Boot 3.5.x (30 modules)
│
└── tracks/
    ├── java25-boot4/
    │   ├── settings.gradle.kts       # included build; its own catalog + build-logic
    │   ├── build-logic/
    │   ├── gradle/libs.versions.toml # Java 25, Boot 4.0.x pinned here
    │   ├── test-support/
    │   ├── README.md                 # what the track covers, how to build, link to docs/tracks/java25-boot4/
    │   └── modules/
    │       ├── 01-core-java/         # same NN-slug as baseline when extending a baseline topic
    │       ├── 03-concurrency/
    │       ├── 07-spring-transactions/
    │       └── 40-whats-new/         # track-only modules use numbers ≥ 40
    │
    └── kotlin/
        ├── settings.gradle.kts
        ├── build-logic/
        ├── gradle/libs.versions.toml # Kotlin 2.x, coroutines, Boot 3.5.x
        ├── test-support/
        ├── README.md
        └── modules/
            ├── 01-core-java/         # → "Core Kotlin for Java engineers"
            ├── 03-concurrency/       # → coroutines, structured concurrency, dispatchers
            ├── 07-spring-transactions/
            ├── 08-jpa-hibernate/
            ├── 21-webclient-webflux/ # → coroutines + WebFlux
            └── 40-kotlin-spring-idioms/
```

Docs:

```text
docs/
├── topics/<slug>/                    # baseline (unchanged)
└── tracks/
    ├── index.md                      # what tracks are, comparison matrix, how to choose
    ├── java25-boot4/
    │   ├── index.md                  # overview, migration guide, what's covered
    │   ├── whats-new-java.md         # 22 → 25 language/runtime changes that matter for backend
    │   ├── whats-new-spring-boot.md  # 3.5 → 4.0 changes, removals, new defaults
    │   ├── migration.md              # step-by-step upgrade + pitfalls + senior questions
    │   └── <slug>/                   # same page set as module-conventions.md §5, delta-scoped
    └── kotlin/
        ├── index.md
        ├── kotlin-for-java-engineers.md
        ├── spring-with-kotlin.md
        └── <slug>/
```

Root `settings.gradle.kts`:

```kotlin
includeBuild("tracks/java25-boot4")
includeBuild("tracks/kotlin")
```

Root aliases (in root `build.gradle.kts`): `buildTracks`, `buildTrack-<id>`, so
`scripts/verify-all.sh` can run `./gradlew build integrationTest compileBrokenExamples buildTracks`.

Java package roots per track: baseline `lab.<topic>`; tracks `lab.<segment>.<topic>` using the
package segment from the §1 registry (`lab.java25boot4.springtransactions`,
`lab.kotlin.concurrency`) to avoid collisions in IDE indexing.

## 4. Track module scope

A track module for an existing baseline slug contains **only the delta**:

```text
tracks/java25-boot4/modules/07-spring-transactions/
├── README.md                    # "Differences from baseline module 07" + links
├── broken-examples/             # broken examples that are NEW or CHANGED on this stack
└── src/                         # correct implementations using the new APIs/features
```

Its docs pages (`docs/tracks/java25-boot4/spring-transactions/`) have the same page set as a
baseline topic but each page begins with a **Delta** admonition: what is unchanged (link to
baseline), what changed, what is new. Questions include at least 2 senior "migration/upgrade"
questions.

Minimums for a **delta** module (extends a baseline slug) are reduced because the baseline
already carries the bulk:

| Item | Delta module | Track-only module (≥ 40) |
|---|---|---|
| Broken examples | ≥ 2, each new or changed on this stack | code-module DoD (≥ 3) |
| Questions basic / intermediate / senior / scenario | 4 / 4 / 4 / 1, of which ≥ 2 senior are migration/upgrade | code-module DoD (8 / 8 / 5 / 2) + ≥ 2 migration |
| Docs pages | full page set, every page Delta-scoped | full page set |
| Everything else (two views, correct code + tests, catalogue registration, `Track:` tag) | unchanged | unchanged |

## 5. Track: `java25-boot4`

**Java 22 → 25 coverage (what a senior backend engineer must know):**
unnamed variables and patterns; primitive types in patterns; flexible constructor bodies; module
import declarations; compact source files / instance `main`; stream gatherers (`Stream::gather`);
class-file API; structured concurrency (final form) and scoped values; virtual threads
improvements (pinning fix for `synchronized`); Foreign Function & Memory API; generational ZGC
default, Shenandoah/G1 changes; JFR improvements; removal/deprecation of `SecurityManager`,
`Unsafe` memory access, 32-bit x86; `jwebserver`, `jpackage` changes; Markdown Javadoc.

**Spring Boot 3.5 → 4.0 / Spring Framework 7 coverage:** Jakarta EE 11 baseline; JSpecify
null-safety annotations; new HTTP client defaults; API versioning support in Spring MVC/WebFlux;
`RestTestClient`; changes to `@Retryable` / resilience features moving into core; Jackson 3;
removed/deprecated auto-configurations and properties; `HttpServiceProxyFactory` registration
changes; Spring Security 7 changes (lambda-only DSL, removed deprecations); Spring Data 4 changes;
Hibernate 7 / JPA 3.2; Spring Kafka 4, Spring AMQP 4 behaviour changes; Actuator endpoint
changes; observability defaults; Testcontainers/Docker Compose support updates.

**Required track modules (initial):** `01-core-java` (language delta), `03-concurrency`
(structured concurrency, scoped values, virtual threads), `06-spring-mvc` (API versioning,
JSpecify, Jackson 3), `07-spring-transactions` (behaviour verification after upgrade),
`10-rest-api` (native versioning vs manual), `11-spring-security` (Security 7 DSL),
`12-testing` (`RestTestClient`, new test slices), `18-resilience` (core retry vs Resilience4j),
`40-whats-new` (runnable feature tour + migration checklist).

**Broken examples specific to this track (examples):** `synchronized` block assumed to pin
virtual threads (outdated fix that now hurts readability); Jackson 2 configuration silently
ignored under Jackson 3; nullability contract violated against JSpecify annotations; security
config using removed `and()` chaining; retry annotation from the wrong package; API version
negotiation ignoring the framework mechanism.

## 6. Track: `kotlin`

**Coverage — Kotlin for Java backend engineers:** null safety and platform types; data classes vs
records; sealed classes/interfaces and exhaustive `when`; extension functions; scope functions;
immutability (`val`, read-only collections vs immutable); `object`, companion objects; delegation;
coroutines (structured concurrency, `Dispatchers`, `suspend`, cancellation, `Flow`,
exception handling, `runBlocking` pitfalls); coroutines vs virtual threads vs reactive; Kotlin
and JPA (open classes, `kotlin-jpa`/`allopen` plugins, `lateinit`, data classes as entities —
why not); Kotlin and Spring (`kotlin-spring` plugin, constructor injection, `@ConfigurationProperties`
with data classes, bean definition DSL, router DSL, WebFlux + coroutines, `CoroutineCrudRepository`);
Jackson Kotlin module; testing (Kotest or JUnit 5 + MockK, AssertJ still fine); Java interop
(`@JvmStatic`, `@JvmOverloads`, SAM conversion, checked exceptions); build (Kotlin Gradle DSL,
compiler flags `-Xjsr305=strict`, K2 compiler); Kotlin-specific performance considerations
(inline classes, boxing, lambdas).

**Required track modules (initial):** `01-core-java` → "Core Kotlin", `03-concurrency` →
coroutines, `04-spring-core` → Kotlin DI idioms, `07-spring-transactions` → `@Transactional`
with suspend functions and `TransactionalOperator`, `08-jpa-hibernate` → entities in Kotlin,
`12-testing` → MockK/Kotest, `21-webclient-webflux` → coroutines + WebFlux,
`40-kotlin-spring-idioms`.

**Broken examples specific to this track (examples):** `!!` scattered over nullable platform
types; data class as JPA entity (equals/hashCode/`toString` traps, lazy relations); `runBlocking`
inside a coroutine/WebFlux handler; `GlobalScope.launch` fire-and-forget losing exceptions;
`@Transactional` on a `suspend` function with a blocking JPA repository; mutable `var` in
`@ConfigurationProperties`; `catch (e: Exception)` swallowing `CancellationException`;
`lateinit` used to fake DI.

## 7. Definition of Done — track

```text
[ ] Track registered in §1, tracks/<id>/ builds independently with its own catalog & toolchain
[ ] docs/tracks/<id>/index.md + whats-new / migration pages (or kotlin-for-java-engineers / spring-with-kotlin)
[ ] Each required track module meets its DoD (§4 table: delta minimums or full code-module DoD), with Delta admonitions
[ ] ≥ 2 senior migration/upgrade questions per track module
[ ] Track-specific issues registered in docs/issues/ with a "Track" metadata field
[ ] Comparison matrix on docs/tracks/index.md updated (baseline vs tracks)
[ ] docs/progress.md has a section per track
[ ] scripts/verify-all.sh builds the track; mkdocs build --strict green
```

## 8. Keeping tracks current

- When a new Java LTS or Spring Boot major is released, create a new track rather than mutating an
  existing one; retire a track only when the baseline moves to it.
- Baseline moves (e.g. baseline → Java 25 + Boot 4) are deliberate, spec-first changes: update
  `AGENTS.md` §1 and this registry, then migrate `modules/`, then the former delta track becomes
  redundant and is removed.
