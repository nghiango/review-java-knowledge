---
type: support
---

# Module test-support

Shared Testcontainers helpers for the Docker-backed integration tests of the baseline modules.

This is a plain `java-library` (no Spring Boot plugin) and it deliberately contains **no test that
needs Docker**: `./gradlew build` stays Docker-free, and the container helper below is exercised
only by a consumer's `integrationTest` source set. Consumers declare one dependency:

```kotlin
integrationTestImplementation(project(":modules:test-support"))
```

The Testcontainers artifacts are exposed with `api`, so a consumer inherits `PostgreSQLContainer`
and the JUnit 5 extension without redeclaring them.

## Layout

- `src/main/java/lab/testsupport/SharedPostgresContainer.java` — a started singleton
  `PostgreSQLContainer<?>` (`postgres:17-alpine`) shared by every integration test in a JVM. Because
  it starts the container itself it must not also be annotated `@Container`/`@Testcontainers`;
  expose it as a `@ServiceConnection` bean from a `@TestConfiguration` in the test that needs it.

## Related

- [`docs/topics/testing/`](../../docs/topics/testing/index.md) — the Testing module these helpers
  serve.
- [Module 12 — Testing](../12-testing/README.md) — the first consumer.
