# JPA / Hibernate

## Why this matters

Jakarta Persistence (JPA) and Hibernate form the core persistence engine for most enterprise Spring applications. While ORMs drastically accelerate development by bridging object-oriented domain models with relational databases, they introduce severe abstraction leaks. Misunderstandings of Hibernate internals—such as dirty checking snapshots, the 1st/2nd level cache, session lifecycle, lazy loading proxies, and cascading semantics—are among the most frequent sources of critical production incidents: N+1 query storms, memory exhaustion, cartesian product query explosions, and silent data corruption.

## Core Concepts

- [Entity Lifecycle States, PersistenceContext, FlushMode, ID Generation Strategies, and Fetch Types](concepts.md)
- [Dirty Checking Mechanism, Bytecode Enhancement, Proxy Initialization, and 1st/2nd Level Caches](internals.md)

## How it works internally

Discover how Hibernate tracks entity modifications through snapshot comparison, how CGLIB/ByteBuddy proxies intercept method calls to trigger lazy initialization, how JDBC batching coordinates with database sequences, and how first-level cache identity maps ensure reference consistency in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions across Basic, Intermediate, Senior, and Production Scenario levels with dedicated runnable code examples.

## Common Production Problems

N+1 select cascades, cartesian product explosions from multiple eager collections, `LazyInitializationException` outside transactions, memory leaks from long-running batch sessions, and broken `equals()`/`hashCode()` hash set mutations are diagnosed in [Production](production.md).

## Broken Examples

1. [N+1 queries from lazy iteration](code-review.md#1-n1-queries-from-lazy-iteration)
2. [Eager fetching cartesian explosion](code-review.md#2-eager-fetching-anti-pattern)
3. [Generated ID in equals and hashCode](code-review.md#3-equals-and-hashcode-with-generated-id)
4. [Bidirectional JSON recursion](code-review.md#4-bidirectional-json-recursion)
5. [Lazy initialization outside transaction](code-review.md#5-lazy-initialization-outside-transaction)
6. [Cascade ALL on ManyToMany](code-review.md#6-cascade-all-on-manytomany-relationship)
7. [Entity exposed through REST API](code-review.md#7-entity-exposed-through-api)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

- **JOIN FETCH vs DTO Projection:** JOIN FETCH preserves entity management and navigation at the cost of fetching all entity fields; DTO projections minimize network and memory footprints by fetching only required columns directly into Java records.
- **Optimistic vs Pessimistic Locking:** Optimistic locking (`@Version`) achieves high throughput with low contention at the cost of handling retry exceptions; pessimistic locking (`PESSIMISTIC_WRITE`) guarantees isolation for high contention at the expense of database row locks.
- **Single Table vs Joined Inheritance:** Single Table yields top query performance by eliminating joins at the cost of nullable columns; Joined maintains strict normalization at the expense of multi-table joins.

## Production Checklist

- Use `FetchType.LAZY` for all `@ManyToOne` and `@OneToOne` associations (override default EAGER).
- Disable Open Session in View (`spring.jpa.open-in-view=false`) to avoid connection pool starvation.
- Solve N+1 queries using `JOIN FETCH`, `@EntityGraph`, or DTO projections; never rely on sub-queries in loops.
- Implement `equals()` and `hashCode()` using immutable natural business keys or stable proxy-safe identity.
- Use `GenerationType.SEQUENCE` with `allocationSize` (e.g. 50) to enable Hibernate JDBC batching; avoid `IDENTITY` for high-volume writes.
- Never expose JPA entities directly in `@RestController` request/response contracts; use immutable Java records (DTOs).
- Use `StatelessSession` or periodic `flush()` / `clear()` during large batch processing jobs to prevent memory leaks.

## Senior-Level Questions

Explore advanced topics like Hibernate 2nd-level caching topology, `StatelessSession` large-scale ETL processing, bytecode enhancement, and multi-tenant persistence strategies in [Senior Questions](questions.md#senior).

## Exercises

Hands-on JPA/Hibernate katas to practice custom SQL query projections, entity graph optimizations, and multi-threaded optimistic locking retry mechanisms in [Exercises](exercises.md).

## Related

- [Spring Transactions](../spring-transactions/index.md)
- [Database Issues](../../issues/database.md)
- [Performance Issues](../../issues/performance.md)
- [Reliability Issues](../../issues/reliability.md)
- [Security Issues](../../issues/security.md)
