---
type: code
topic: database-sql
---

# Module 09: Database / SQL

Relational modeling, indexing strategies (B-tree, composite, covering, partial), query execution plans (`EXPLAIN ANALYZE BUFFERS`), isolation anomalies, locking mechanisms (MVCC, pessimistic row locks, advisory locks, `SKIP LOCKED`), pagination patterns, and zero-downtime database migrations.

## Canonical Documentation

Prose, theory, interview questions, architecture diagrams, and exercises are authored in the docs site:
[Database / SQL Topic Documentation](../../docs/topics/database-sql/index.md)

## Layout

- `broken-examples/`: Clean review targets with realistic database bottlenecks and locking bugs.
- `src/main/java/lab/databasesql/`: Correct production implementations using Spring `JdbcClient`, keyset pagination, atomic updates, and expand-contract migrations.
- `src/examples/java/lab/databasesql/questions/`: Dedicated compilable question examples with trailing evaluation result comments (`Q01` through `Q23`).
- `src/test/java/lab/databasesql/`: Fast unit test suites.
