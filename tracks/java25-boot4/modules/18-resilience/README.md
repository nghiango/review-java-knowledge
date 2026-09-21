# Module: Java 25 & Spring Boot 4 Track — 18 Resilience

Delta track module covering resilience modernization under Spring Boot 4.0 / Spring Framework 7 and Java 25:

- **Core Framework Retry vs Resilience4j**: Evolution of retry primitives in modern Spring Framework core vs external Resilience4j decorators.
- **Virtual Thread Friendly Timeouts & Concurrency Limiters**: Eliminating carrier thread pinning during timeout execution and replacing semaphore/thread-pool bulkheads with non-blocking concurrency limiters.
- **Adaptive Backoff with Jitter & Circuit Breakers**: Bounded exponential backoff with full randomized jitter preventing thundering herds on microservice failovers.

Canonical documentation: [`docs/tracks/java25-boot4/resilience/index.md`](../../../../docs/tracks/java25-boot4/resilience/index.md).
