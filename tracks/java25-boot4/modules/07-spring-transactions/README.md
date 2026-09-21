# Java 25 & Spring Boot 4 Track: Spring Transactions Delta

This module covers transaction management verification in modern Spring Boot 4.0 / Spring Framework 7 and Java 25:
- **Transaction Boundaries across Virtual Threads & Structured Scopes**: Understanding why Spring transactions are thread-bound (`ThreadLocal`), the dangers of invoking `StructuredTaskScope` or spawning virtual threads inside `@Transactional` blocks, and clean orchestration patterns.
- **Scoped Value Transaction Coordination**: Using Java 25 `ScopedValue` alongside `TransactionTemplate` for zero-cleanup, leak-proof context sharing across concurrent virtual worker pipelines.
- **Transactional Event Listeners on Virtual Threads**: Verifying post-commit (`AFTER_COMMIT`) decoupled event processing.

See the canonical track documentation in [`docs/tracks/java25-boot4/spring-transactions/`](../../../../docs/tracks/java25-boot4/spring-transactions/).
