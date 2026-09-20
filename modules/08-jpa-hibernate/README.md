# Module 08 — JPA / Hibernate

This module covers Object-Relational Mapping (ORM), entity lifecycles, persistence contexts, dirty checking, relationships, N+1 query diagnostics, fetch strategies, DTO projections, and locking models.

Canonical theory, interview questions, production failure scenarios, and code review exercises are documented in [docs/topics/jpa-hibernate/](../../docs/topics/jpa-hibernate/index.md).

## Running Tests and Verification

```bash
./gradlew :modules:08-jpa-hibernate:test
./gradlew :modules:08-jpa-hibernate:compileBrokenExamples
./gradlew :modules:08-jpa-hibernate:compileExamples
```
