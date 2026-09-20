# Spring Transactions

## Why this matters

Transactions form the bedrock of data integrity in enterprise applications. Understanding Spring's declarative transaction management (`@Transactional`), proxy interceptor chains, `PlatformTransactionManager`, `TransactionSynchronizationManager`, connection pool lifecycles (HikariCP), propagation semantics, and rollback rules is critical. Misunderstandings frequently lead to severe production outages: connection pool starvation from remote HTTP calls inside transactions, silent data corruption from self-invocation bypasses or checked exception commits, and deadlock from misuse of `Propagation.REQUIRES_NEW`.

## Core Concepts

- [PlatformTransactionManager, TransactionDefinition, Propagation, Isolation Levels, Rollback Rules, TransactionSynchronizationManager, and HikariCP](concepts.md)
- [TransactionInterceptor, ReflectiveMethodInvocation, TransactionSynchronizationManager connection binding, and commit/rollback pipeline](internals.md)

## How it works internally

Follow how Spring intercepts `@Transactional` methods through `TransactionInterceptor`, acquires connections from HikariCP, binds them to `ThreadLocal` in `TransactionSynchronizationManager`, manages propagation contexts, executes before/after commit synchronizations, and releases database resources in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions: 8 Basic, 8 Intermediate, 5 Senior, and 2 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

HikariCP connection pool starvation from long-running remote API calls inside transactions, silent transaction bypass from `this` self-invocation, unexpected commits on checked exceptions, `UnexpectedRollbackException` from swallowed inner transaction exceptions, and asynchronous thread boundary disconnection are diagnosed in [Production](production.md).

## Broken Examples

1. [Self-invocation transaction bypass](code-review.md#self-invocation-transaction-bypass)
2. [Remote API call inside transaction](code-review.md#remote-api-call-inside-transaction)
3. [Checked exception rollback assumption](code-review.md#checked-exception-rollback-assumption)
4. [Wrong propagation and connection pool exhaustion](code-review.md#wrong-propagation-and-connection-pool-exhaustion)
5. [Asynchronous execution across transaction boundaries](code-review.md#asynchronous-execution-across-transaction-boundaries)
6. [Transaction on private method](code-review.md#transaction-on-private-method)
7. [Comprehensive order processing workflow PR](code-review.md#comprehensive-order-processing-workflow-pr)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

Declarative `@Transactional` provides clean separation of concerns but requires awareness of proxy boundaries and propagation rules; `TransactionTemplate` provides precise programmatic scoping at the cost of boilerplate; `Propagation.REQUIRES_NEW` creates autonomous transactions while consuming multiple database connections concurrently per thread.

## Production Checklist

- Never execute slow remote HTTP calls, third-party APIs, or heavy file I/O inside `@Transactional` boundaries.
- Cross-cutting transactional methods must be invoked across collaborator bean boundaries to pass through Spring AOP proxies.
- Declare `rollbackFor = Exception.class` or use domain `RuntimeException` hierarchies when checked exceptions should trigger rollback.
- Avoid `Propagation.REQUIRES_NEW` inside loops or under constrained HikariCP pool sizes to prevent connection pool exhaustion deadlocks.
- Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for external side effects (emails, webhooks, Kafka messages).
- Mark read-only queries with `@Transactional(readOnly = true)` to disable Hibernate dirty checking and enable read-replica routing.

## Senior-Level Questions

Explore advanced topics like custom `PlatformTransactionManager` chaining, distributed Saga patterns vs 2PC, `ThreadLocal` transaction state propagation, and transactional outbox patterns in [Senior Questions](questions.md#senior).

## Exercises

Hands-on Spring Transaction katas to practice custom transaction synchronizations, programmatic savepoints, and after-commit event listeners in [Exercises](exercises.md).

## Related

- [Spring Core](../spring-core/index.md)
- [Spring Boot](../spring-boot/index.md)
- [Spring MVC](../spring-mvc/index.md)
- [Transaction Issues](../../issues/transaction.md)
- [Reliability Issues](../../issues/reliability.md)
- [Performance Issues](../../issues/performance.md)
- [Maintainability Issues](../../issues/maintainability.md)
