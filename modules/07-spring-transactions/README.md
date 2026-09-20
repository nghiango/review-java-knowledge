# Module 07 — Spring Transactions

This module covers Spring Framework transaction management, `@Transactional` proxy mechanics, propagation levels, rollback rules, isolation levels, HikariCP connection pool lifecycles, and transaction synchronization.

Canonical theory, interview questions, production failure scenarios, and code review exercises are documented in [docs/topics/spring-transactions/](../../docs/topics/spring-transactions/index.md).

## Running Tests and Verification

```bash
./gradlew :modules:07-spring-transactions:test
./gradlew :modules:07-spring-transactions:compileBrokenExamples
./gradlew :modules:07-spring-transactions:compileExamples
```
