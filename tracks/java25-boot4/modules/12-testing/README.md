# Java 25 & Spring Boot 4 Track: Testing Delta

This module covers testing modernization in Spring Boot 4.0 / Spring Framework 7 and Java 25:
- **`RestTestClient`**: Unified, modern testing client for Spring MVC REST endpoints, replacing `MockMvc` and `WebTestClient` fragmentation with fluent assertion ergonomics.
- **Deterministic Virtual Thread Testing**: Eliminating `Thread.sleep` anti-patterns, utilizing Awaitility for poll-based synchronization, and verifying `StructuredTaskScope` subtask failure handling.
- **RFC 9457 Problem Details & JSpecify Assertions**: Verifying error contracts and static null-safety guarantees in unit and integration test fixtures.
- **ArchUnit Rules for Java 25**: Architectural fitness tests enforcing `@NullMarked` package contracts and prohibiting carrier thread pinning.

See the canonical track documentation in [`docs/tracks/java25-boot4/testing/`](../../../../docs/tracks/java25-boot4/testing/).
