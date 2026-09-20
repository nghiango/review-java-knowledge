# Module 18 — Resilience

This module contains practical implementations, unit tests, and broken code review exercises for
fault-tolerant microservices: connect/read/execution timeouts, bounded retries with exponential backoff and randomized jitter,
Circuit Breaker sliding windows and state transitions, Bulkhead concurrency isolation, Rate Limiting (Token Bucket / Leaky Bucket),
Load Shedding, Fallbacks & Graceful Degradation, and Resilience4j aspect ordering.

Full theory, concepts, internals diagrams, interview questions, and deep walkthroughs live in the documentation:

👉 **[Resilience Documentation](../../docs/topics/resilience/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed resilience implementations for code review practice
  - `infinite-retry/`: Infinite retry loop on external payment API without maximum attempts or backoff
  - `retry-without-timeout/`: Retrying external HTTP calls without socket/read timeouts pinning threads
  - `retrying-non-idempotent-call/`: Retrying non-idempotent mutation calls without idempotency keys causing double billing
  - `retry-storm-missing-jitter/`: Fixed-interval retries without jitter creating synchronized thundering herd storms
  - `catch-and-ignore-failures/`: Swallowing exceptions in fallbacks blinding circuit breakers and monitoring
- `src/main/java/lab/resilience/`: Production-grade correct implementations
- `src/test/java/lab/resilience/`: Fast unit tests for retries, timeouts, jitter, and circuit breaker transitions
- `src/integrationTest/java/lab/resilience/`: WireMock HTTP fault-injection integration tests
- `src/examples/java/lab/resilience/questions/`: Standalone compilable question verification classes
