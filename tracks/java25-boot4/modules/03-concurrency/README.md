# Java 25 & Spring Boot 4 Track: Concurrency Delta

This module covers the modern concurrency delta in Java 25:
- **Structured Concurrency (`StructuredTaskScope`)**: Eliminating orphan subtasks and coordinating cancellation via `ShutdownOnFailure` and `ShutdownOnSuccess` (JEP 480).
- **Scoped Values (`ScopedValue`)**: Immutable, stack-bounded context inheritance across virtual threads replacing memory-heavy `ThreadLocal` (JEP 481).
- **Virtual Thread Synchronization Unpinning**: Verification that `synchronized` monitor blocks no longer pin virtual threads to carrier threads in Java 25.

See the canonical track documentation in [`docs/tracks/java25-boot4/concurrency/`](../../../../docs/tracks/java25-boot4/concurrency/).
