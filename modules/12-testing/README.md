# Module 12 — Testing

This module contains theory, runnable review exercises, unit and integration tests, and production-grade implementations covering automated testing for Spring backends:
- Behaviour-focused assertions instead of assertions coupled to implementation details
- Contract verification against a stub server instead of mocking the integration boundary
- Isolated, deterministic fixtures and test data builders instead of shared mutable state
- Asynchronous assertions with Awaitility bounded timeouts instead of `Thread.sleep`
- PostgreSQL Testcontainers (`@ServiceConnection`) instead of in-memory substitutes that diverge from production semantics
- Order-independent, parallel-safe suites and shared container singletons

## Commands

```bash
./gradlew :modules:12-testing:test                  # fast unit tests (no Docker)
./gradlew :modules:12-testing:integrationTest       # Testcontainers / @SpringBootTest (Docker)
./gradlew :modules:12-testing:compileExamples       # compile question/demo examples
./gradlew :modules:12-testing:compileBrokenExamples # compile the review targets
```

## Canonical Documentation
See [`docs/topics/testing/`](../../docs/topics/testing/index.md) for full theory, question banks, code reviews, and production guides.
