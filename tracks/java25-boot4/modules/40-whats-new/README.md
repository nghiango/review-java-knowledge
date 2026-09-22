# Track Module: 40 — What's New (Feature Tour & Migration Checklist)

A runnable tour of the **Java 22 → 25** and **Spring Boot 3.5 → 4.0** changes that a senior
backend engineer must know, plus the migration hazards that turn a version bump into an incident.

This is a **track-only** module (`≥ 40`): it does not extend a baseline topic. It complements the
track-level pages [`whats-new-java.md`](../../../docs/tracks/java25-boot4/whats-new-java.md) and
[`whats-new-spring-boot.md`](../../../docs/tracks/java25-boot4/whats-new-spring-boot.md) with code
you can run and review.

## What is here

- **Runnable feature tour** — `FeatureTour` prints and demonstrates the final features that matter
  for a service: stream gatherers, the Foreign Function & Memory API, scoped values.
- **Broken examples** — realistic upgrade pull requests that compile on the new stack but are wrong:
  - `unsafe-offheap-buffer` — `sun.misc.Unsafe` off-heap memory that leaks and corrupts.
  - `security-manager-authorization` — authorization built on the disabled `SecurityManager`.
  - `boot4-renamed-property-silent` — a configuration property that silently stops binding.
- **Correct implementations** — tested counterparts under `src/main/java/lab/java25boot4/whatsnew/`.

## How to run

```bash
cd tracks/java25-boot4
../../gradlew :modules:40-whats-new:test
../../gradlew :modules:40-whats-new:compileBrokenExamples
../../gradlew :modules:40-whats-new:compileExamples
```

The tour itself:

```bash
cd tracks/java25-boot4
../../gradlew :modules:40-whats-new:runFeatureTour
```

## Documentation

Canonical prose, migration checklist and interview Q&A:

👉 **[What's New — Feature Tour](../../../docs/tracks/java25-boot4/whats-new/index.md)**
