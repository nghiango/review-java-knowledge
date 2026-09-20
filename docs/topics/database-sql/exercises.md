# Database / SQL Practice Exercises

Solidify your SQL optimization, locking, and schema design skills with these practical engineering katas.

---

## Kata 1: Execution Plan Optimization

### Objective
Given a slow reporting query joining three tables (`customers`, `orders`, `order_items`) that currently executes a Sequential Scan and Hash Join:
1. Run `EXPLAIN (ANALYZE, BUFFERS)` to diagnose buffer cache misses and disk reads.
2. Design composite and covering indexes with `INCLUDE` clauses to transform the plan into an **Index Only Scan**.
3. Verify that total execution time drops from $> 500\text{ms}$ to $< 5\text{ms}$.

---

## Kata 2: High-Throughput Worker Queue with `SKIP LOCKED`

### Objective
Build a multi-threaded task dispatcher backed by a PostgreSQL database table `job_queue`.

### Requirements
- 20 concurrent Spring worker threads pulling jobs continuously.
- Use `SELECT ... FOR UPDATE SKIP LOCKED` inside a Common Table Expression to guarantee that no two workers attempt to claim the same job.
- Verify zero deadlock occurrences and $100\%$ task completion.

---

## Kata 3: Zero-Downtime Expand-Contract Column Migration

### Objective
Perform an online schema evolution splitting a monolithic `full_name VARCHAR(255)` column into `first_name VARCHAR(100)` and `last_name VARCHAR(100)` on a table with 10 million rows.

### Requirements
- Write the 4 Flyway migration scripts corresponding to Expand, Backfill, Switch, and Contract.
- Maintain dual-write synchronization during the migration window.
- Ensure that active V1 application queries continue to function without errors throughout the deployment.
