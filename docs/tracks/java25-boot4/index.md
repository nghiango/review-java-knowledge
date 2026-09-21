# Track: Java 25 & Spring Boot 4

Delta track for upgrading a Java 21 / Spring Boot 3.5 service to **Java 25 LTS** and
**Spring Boot 4.0 / Spring Framework 7**. The baseline explains how Spring works; this track
explains what is different on the newer stack — and what breaks on the way there.

!!! info "Delta from baseline"
    Unchanged: the 30 baseline topics, their theory and their correct implementations →
    [baseline topics](../../topics/core-java/index.md)
    Changed: language features, framework APIs, auto-configuration defaults, test slices
    New: stream gatherers, structured concurrency, scoped values, unpinned `synchronized`,
    JSpecify nullness, API versioning, `RestTestClient`, Jackson 3

## Start here

1. [What's new in Java 22 → 25](whats-new-java.md) — language and runtime changes that matter for
   backend services.
2. [What's new in Spring Boot 3.5 → 4.0](whats-new-spring-boot.md) — removals, new defaults and
   framework behaviour changes.
3. [Migration guide](migration.md) — a step-by-step upgrade with pitfalls and the senior questions
   to expect.

## Modules

Only the delta is implemented per module. Each module keeps the baseline's number and slug.

| Module | Topic | Status | Delta focus |
|---|---|---|---|
| [01-core-java](core-java/index.md) | Core Java | Delta implemented | stream gatherers, flexible constructor bodies, unnamed variables, primitive patterns, unpinned virtual threads |
| [03-concurrency](concurrency/index.md) | Concurrency | Delta implemented | structured concurrency, scoped values, virtual thread scheduling |
| [06-spring-mvc](spring-mvc/index.md) | Spring MVC | Delta implemented | API versioning, JSpecify nullness, Jackson 3 |
| [07-spring-transactions](spring-transactions/index.md) | Spring Transactions | Delta implemented | transactional behaviour re-verification after the upgrade |
| [10-rest-api](rest-api/index.md) | REST API | Delta implemented | declarative @HttpExchange, native versioning vs hand-rolled, RFC 8594 headers |
| [11-spring-security](spring-security/index.md) | Spring Security | Delta implemented | Security 7 lambda-only DSL, virtual thread context propagation with ScopedValue |
| [12-testing](testing/index.md) | Testing | Delta implemented | unified REST test client assertions, deterministic virtual thread tests with Awaitility, ArchUnit rules |
| 18-resilience | Resilience | Planned | core retry vs Resilience4j |
| 40-whats-new | Feature tour | Planned | runnable tour + migration checklist |

## Stack

| Element | Value |
|---|---|
| Language / runtime | Java 25 LTS (toolchain `JavaLanguageVersion.of(25)`) |
| Framework | Spring Boot 4.0.x / Spring Framework 7.x (Jakarta EE 11 baseline) |
| Package root | `lab.java25boot4.<topic>` |
| Catalog | `tracks/java25-boot4/gradle/libs.versions.toml` (isolated from the baseline) |
| Build | included build — `./gradlew buildTrack-java25-boot4` |

## Build

```bash
./gradlew buildTrack-java25-boot4          # from the repository root
cd tracks/java25-boot4 && ../../gradlew build compileBrokenExamples
```

Preview features are enabled with `--enable-preview` by the track's convention plugin, because
primitive type patterns and structured concurrency are still preview APIs on Java 25.

!!! warning "Toolchain"
    The track requires a **JDK 25** toolchain. Gradle resolves it through the
    `foojay-resolver-convention` plugin, so the first build may download a JDK.

## Related

- [Version and language tracks](../index.md)
- [Tracks specification](../../spec/tracks.md)
- [Baseline Core Java](../../topics/core-java/index.md)
