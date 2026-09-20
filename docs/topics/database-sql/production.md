# Database / SQL in Production

## 1. Real-World Incident Postmortems

### Incident 1: Unindexed Foreign Key Lock Cascade Outage
- **Symptom:** Database CPU hit 100%, active connection pool reached maximum limit, and all HTTP endpoints timed out.
- **Root Cause:** A cleanup worker deleted 10,000 old parent `orders` records. Because `order_items(order_id)` lacked an index, PostgreSQL executed 10,000 sequential scans over the 30-million-row `order_items` table and acquired `SHARE ROW EXCLUSIVE` table-level locks, blocking all concurrent user orders.
- **Remediation:** Added `CREATE INDEX CONCURRENTLY idx_order_items_order_id ON order_items(order_id);`.
- **Prevention:** Added CI database linter to verify that all foreign key columns have matching indexes.

---

### Incident 2: Lost Updates in High-Concurrency Wallet Service
- **Symptom:** End-of-day ledger reconciliation showed a $45,000 discrepancy across active customer accounts.
- **Root Cause:** Wallet debit endpoint read current balance into Java memory, computed `newBalance = balance - amount`, and wrote back using `UPDATE accounts SET balance = :newBalance`. Two concurrent withdrawals read the same balance and overwrote each other.
- **Remediation:** Replaced application read-modify-write with single atomic SQL updates:
  ```sql
  UPDATE accounts SET balance = balance - :amount WHERE id = :id AND balance >= :amount;
  ```

---

### Incident 3: Production Crash from Single-Phase Column Rename
- **Symptom:** 100% of checkout requests failed with `PSQLException: column "shipping_address" does not exist` immediately after CI/CD pipeline deployed Flyway migration.
- **Root Cause:** The migration renamed the column before the new application version was fully rolled out. In-flight and existing V1 pods still executed queries against `shipping_address`.
- **Remediation:** Rolled forward using the Expand and Contract pattern with a backward-compatible view.

---

## 2. Production Telemetry and Diagnostics

### Detecting Slow Queries with `pg_stat_statements`
Enable the `pg_stat_statements` extension to track query execution metrics:

```sql
SELECT 
    query, 
    calls, 
    round(total_exec_time::numeric, 2) AS total_ms,
    round(mean_exec_time::numeric, 2) AS mean_ms,
    round((100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0))::numeric, 2) AS hit_ratio
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

### Inspecting Lock Contention and Blocked Queries
Query `pg_stat_activity` to diagnose active lock queues:

```sql
SELECT 
    blocked_locks.pid     AS blocked_pid,
    blocked_activity.usename  AS blocked_user,
    blocking_locks.pid    AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query    AS blocked_statement,
    blocking_activity.query   AS current_statement_in_blocking_process
FROM  pg_catalog.pg_locks         blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks         blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

---

## 3. Production Hardening Checklist

- [ ] Ensure all foreign key columns have corresponding B-Tree indexes.
- [ ] Set `statement_timeout` (e.g. `30s`) and `idle_in_transaction_session_timeout` (e.g. `10s`).
- [ ] Configure HikariCP connection pool size using the formula: $((N_{\text{cores}} \times 2) + 1)$.
- [ ] Monitor dead tuple count and vacuum thresholds in `pg_stat_user_tables`.
- [ ] Implement Keyset pagination for high-volume list endpoints.
- [ ] Execute all schema migrations using the multi-phase Expand and Contract pattern.
