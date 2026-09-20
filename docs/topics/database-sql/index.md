# Database / SQL

## Why this matters

The relational database is the single source of truth for enterprise backend systems. While ORMs abstract query syntax, real-world scalability and reliability depend directly on deep SQL knowledge: query planner execution strategies, B-tree composite index ordering, covering index scans, ACID isolation anomalies, transaction lock hierarchies, MVCC garbage collection, and zero-downtime schema evolution. Misconceptions in these areas lead directly to production catastrophes: database CPU saturation from sequential scans on unindexed foreign keys, connection pool starvation from long transactions holding row locks, lost updates from un-synchronized read-modify-write cycles, and outages from destructive schema migrations.

## Core Concepts

- [Relational Modeling, B-Tree Indexes, Composite Index Ordering, Covering & Partial Indexes, Joins, and Keyset Pagination](concepts.md)
- [PostgreSQL MVCC Internals, Tuple Visibility (xmin/xmax), Lock Hierarchies, WAL, Autovacuum, and EXPLAIN ANALYZE BUFFERS](internals.md)

## How it works internally

Discover how PostgreSQL resolves queries using B-Tree index traversal, how `EXPLAIN (ANALYZE, BUFFERS)` reveals disk reads versus shared buffer cache hits, how MVCC coordinates multi-version snapshots without reader/writer lock contention, and how the autovacuum daemon prevents transaction ID wraparound in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions across Basic, Intermediate, Senior, and Production Scenario levels with dedicated runnable code examples.

## Common Production Problems

Full table scans on unindexed foreign keys, slow deep `OFFSET` pagination, lost updates from non-atomic balance modifications, prolonged row locks blocking connection pools, SQL injection via string concatenation, and downtime caused by direct column renaming are diagnosed in [Production](production.md).

## Broken Examples

1. [Unindexed foreign key causing sequential scans](code-review.md#1-unindexed-foreign-key-and-missing-index)
2. [Wrong composite index column order](code-review.md#2-wrong-composite-index-column-order)
3. [Deep offset pagination performance bottleneck](code-review.md#3-deep-offset-pagination-performance)
4. [Lost update anomaly without locking](code-review.md#4-lost-update-without-locking)
5. [Long transaction holding exclusive row locks](code-review.md#5-long-transaction-holding-locks)
6. [SQL injection via string concatenation](code-review.md#6-sql-injection-via-string-concatenation)
7. [Destructive database migration](code-review.md#7-destructive-database-migration)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

- **B-Tree Indexing vs Write Overhead:** Indexes drastically accelerate SELECT lookups from $O(N)$ to $O(\log N)$ at the cost of slower INSERT/UPDATE/DELETE operations and disk space overhead.
- **OFFSET vs Keyset Pagination:** `OFFSET` supports arbitrary page jumping at the cost of $O(N)$ linear degradation and pagination drift; Keyset pagination guarantees constant $O(1)$ latency and stability at the cost of requiring sequential navigation.
- **Optimistic vs Pessimistic Locking:** Optimistic locking provides superior throughput under low contention; pessimistic row locking (`SELECT FOR UPDATE`) or atomic SQL decrement guarantees integrity under high contention at the cost of database lock overhead.

## Production Checklist

- Always create explicit B-Tree indexes on foreign key columns referencing parent tables.
- Structure composite indexes using the **Equality-Then-Range** rule (`(equality_col, range_or_sort_col)`).
- Replace `OFFSET` pagination on large tables with Keyset / Cursor-based seek pagination.
- Use atomic single-statement SQL updates (`UPDATE ... SET balance = balance - :amount WHERE id = :id AND balance >= :amount`) for balance and inventory mutations.
- Keep database transactions as short as possible; never execute remote HTTP/REST calls while holding database row locks.
- Always use parameterized queries (`JdbcClient` / `NamedParameterJdbcTemplate`) to eliminate SQL injection and enable prepared statement caching.
- Execute schema changes using the 4-phase **Expand and Contract** pattern to guarantee zero-downtime rolling deployments.

## Senior-Level Questions

Explore advanced topics like PostgreSQL advisory locks, table partitioning strategies, `SELECT FOR UPDATE SKIP LOCKED` worker queues, and online migration of 100M-row tables in [Senior Questions](questions.md#senior).

## Exercises

Hands-on SQL performance and concurrency katas to practice query tuning with `EXPLAIN (ANALYZE, BUFFERS)`, multi-worker queue processing with `SKIP LOCKED`, and zero-downtime column migrations in [Exercises](exercises.md).

## Related

- [JPA / Hibernate](../jpa-hibernate/index.md)
- [Spring Transactions](../spring-transactions/index.md)
- [Database Issues](../../issues/database.md)
- [Performance Issues](../../issues/performance.md)
- [Concurrency Issues](../../issues/concurrency.md)
- [Security Issues](../../issues/security.md)
