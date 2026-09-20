---
type: code
---

# Module 03 — Concurrency

Production-focused Java 21 concurrency engineering covering the Java Memory Model, happens-before
guarantees, synchronization primitives, lock-free algorithms, executors and bounded queues,
`CompletableFuture` composition pipelines, deadlock prevention, and Java 21 Virtual Threads & `ScopedValue`.

Study the canonical prose in [`docs/topics/concurrency/`](../../docs/topics/concurrency/).

```bash
./gradlew :modules:03-concurrency:test
./gradlew :modules:03-concurrency:compileBrokenExamples
./gradlew :modules:03-concurrency:compileExamples
```

This is a core concurrency module and has no Spring Boot `bootRun` application.
