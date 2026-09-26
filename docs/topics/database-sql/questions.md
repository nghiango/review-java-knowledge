# Database / SQL Interview Questions & Answers

<!-- --8<-- [start:basic] -->
## Basic

### 1. What are database Normal Forms (1NF, 2NF, 3NF, BCNF) and when should you intentionally denormalize?

??? question "Reveal answer"
    - **1NF:** Atomic values, no repeating groups.
    - **2NF:** 1NF + No partial dependencies on composite primary keys.
    - **3NF:** 2NF + No transitive dependencies on non-key attributes.
    - **BCNF:** Strict 3NF where every determinant is a candidate key.
    
    Intentional denormalization duplicates data across tables to eliminate expensive multi-table joins in high-volume read workloads, trading storage and write complexity for query latency.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q01RelationalNormalization.java"
        ```

### 2. How does a B-Tree index work internally and why is its search complexity $O(\log N)$?

??? question "Reveal answer"
    A B-Tree stores sorted keys in balanced multi-level 8 KB pages (root, branch, leaf). Searching starts at the root page, performs binary search to find the appropriate child branch pointer, and traverses down to the leaf page containing Tuple Identifiers (TIDs). A 3-level tree with fan-out of 500 can index over 125 million rows in just 3 page reads.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q02BTreeIndexInternals.java"
        ```

### 3. What is the Leftmost Prefix Rule for composite indexes?

??? question "Reveal answer"
    A composite index `(A, B, C)` can only be used for direct range and equality lookups if the query's `WHERE` clause filters on the leading (leftmost) column `A`. Filtering on `B` or `C` alone cannot traverse the B-Tree root and branch hierarchy directly, forcing full index scans or sequential scans.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q03CompositeIndexLeftmostRule.java"
        ```

### 4. What is a Covering Index (Index-Only Scan) and how does index selectivity affect plan selection?

??? question "Reveal answer"
    A covering index contains all columns requested by a query (using `CREATE INDEX ... INCLUDE (...)`). The database retrieves data entirely from B-Tree leaf pages without touching the heap table pages. When selectivity is high (matching $< 5\%$ of rows), the planner chooses Index Scans; when selectivity is low, it chooses Sequential Scans to minimize random I/O.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q04CoveringIndexSelectivity.java"
        ```

### 5. When should you use Partial Indexes and Functional/Expression Indexes?

??? question "Reveal answer"
    - **Partial Index:** Indexes rows matching a predicate (`WHERE status = 'PENDING'`). Ideal when only a small fraction of rows are actively queried, drastically reducing index size and write overhead.
    - **Functional Index:** Indexes the evaluated result of a function (`LOWER(email)`), enabling fast indexed lookups on case-insensitive or transformed columns.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q05PartialAndFunctionalIndexes.java"
        ```

### 6. How do you interpret an `EXPLAIN (ANALYZE, BUFFERS)` execution plan?

??? question "Reveal answer"
    `EXPLAIN` estimates query costs, while `ANALYZE` actually executes the query to output real elapsed execution time and row counts. `BUFFERS` reports I/O activity: `shared hit` indicates 8 KB pages retrieved directly from RAM (`shared_buffers`), while `read` indicates physical disk reads.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q06ExplainAnalyzeBuffers.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 7. Compare the three physical SQL join algorithms (Nested Loop, Hash Join, Merge Join).

??? question "Reveal answer"
    - **Nested Loop:** Iterates over the outer table and seeks inner table rows via an index. Best for small outer datasets with indexed inner keys.
    - **Hash Join:** Hashes the smaller relation in memory, then streams the larger relation to probe matching keys. Best for large unindexed equality joins.
    - **Merge Join:** Scans two pre-sorted relations simultaneously in linear time. Best when both datasets are already sorted by B-Tree indexes.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q07SqlJoinAlgorithms.java"
        ```

### 8. How do relational storage engines implement ACID guarantees?

??? question "Reveal answer"
    - **Atomicity & Durability:** Implemented via Write-Ahead Logging (WAL) and commit log records (`fsync` to disk).
    - **Consistency:** Enforced by constraint checks, schema rules, and foreign key validations.
    - **Isolation:** Implemented via Multi-Version Concurrency Control (MVCC) snapshot isolation and relational row/table locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q08AcidGuarantees.java"
        ```

### 9. What are the four database isolation levels and their corresponding anomalies?

??? question "Reveal answer"
    - **READ UNCOMMITTED:** Subject to Dirty Reads, Non-Repeatable Reads, and Phantoms.
    - **READ COMMITTED (Default in PG):** Prevents Dirty Reads; subject to Non-Repeatable Reads and Phantoms.
    - **REPEATABLE READ:** Prevents Dirty, Non-Repeatable, and Phantom Reads in PostgreSQL via transaction start snapshots; subject to Write Skew.
    - **SERIALIZABLE:** Prevents all anomalies including Write Skew via Serializable Snapshot Isolation (SSI).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q09IsolationAnomalies.java"
        ```

### 10. How does PostgreSQL MVCC work and why is Autovacuum essential?

??? question "Reveal answer"
    PostgreSQL attaches `xmin` (creating TXID) and `xmax` (deleting/updating TXID) to every row header. Updating a row writes a new tuple version without in-place mutation. Autovacuum reclaims space occupied by dead tuples, updates the Free Space Map and Visibility Map, and prevents the 2-billion Transaction ID wraparound catastrophe.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q10MvccAndVacuumInternals.java"
        ```

### 11. How does the relational locking hierarchy operate in PostgreSQL?

??? question "Reveal answer"
    Locks exist at row, table, and advisory levels. `ACCESS SHARE` (acquired by `SELECT`) conflicts only with `ACCESS EXCLUSIVE` (acquired by `ALTER TABLE` / `DROP TABLE`). `ROW EXCLUSIVE` (acquired by `UPDATE`/`DELETE`) conflicts with table-level share locks. `SELECT ... FOR UPDATE` acquires row-level exclusive locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q11LockingHierarchy.java"
        ```

### 12. How does PostgreSQL detect deadlocks and how do you prevent them?

??? question "Reveal answer"
    When a lock wait exceeds `deadlock_timeout` (default: 1 second), PostgreSQL analyzes the active Wait-For Graph. If a directed cycle is found, it aborts one transaction with SQLSTATE `40P01`. Prevent deadlocks by enforcing **canonical resource ordering** (e.g. always acquiring locks in ascending primary key order).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q12DeadlockDetectionAndResolution.java"
        ```

### 13. What is the difference between `SELECT FOR UPDATE`, `NOWAIT`, and `SKIP LOCKED`?

??? question "Reveal answer"
    - `FOR UPDATE`: Blocks until the locked row is released.
    - `FOR UPDATE NOWAIT`: Fails immediately with error `55P03` if the row is already locked.
    - `FOR UPDATE SKIP LOCKED`: Bypasses currently locked rows and returns the next available unlocked rows immediately, enabling lock-free distributed worker queues.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q13SelectForUpdateSkipLocked.java"
        ```

### 14. What are PostgreSQL Advisory Locks and when are they used?

??? question "Reveal answer"
    Advisory locks are application-defined locks identified by 64-bit integer keys. They exist independently of table data. Session-level advisory locks remain active until explicitly unlocked or connection closure; transaction-level advisory locks (`pg_advisory_xact_lock`) release automatically on commit/rollback. Used for distributed schedulers, singleton jobs, and Flyway migration coordination.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q14AdvisoryLocks.java"
        ```

### 24. What are the internal differences and trade-offs between PostgreSQL GIN and GiST indexes?

??? question "Reveal answer"

    **Short Answer:** GIN (Generalized Inverted Index) decomposes composite values into individual elements, mapping each element to a posting list of row IDs; ideal for JSONB containment (`@>`), arrays, and full-text search. GiST (Generalized Search Tree) is a balanced tree of lossy bounding boxes; ideal for geometric coordinates, range types, and nearest-neighbor search.

    **Internal Mechanism:** GIN maintains a B-Tree of distinct element keys with posting trees/lists; writes incur write amplification updating multiple posting lists. GiST uses lossy node predicates (`consistent`), checking matches and re-checking heap tuples via lossy index scans.

    **Common Mistake:** Using standard B-Tree indexes on JSONB columns, which can only evaluate exact JSON equality (`=`) and cannot accelerate sub-document key/value lookups (`@>`). [Concepts](/topics/database-sql/concepts.md#3-covering-partial-and-functional-indexes)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q24GinGistIndexInternalsExample.java"
        ```

### 25. How does the CTE optimization fence (MATERIALIZED vs NOT MATERIALIZED) affect query plans in PostgreSQL 12+?

??? question "Reveal answer"

    **Short Answer:** In PostgreSQL 12+, CTEs (`WITH` clauses) are inlined into the outer query by default (`NOT MATERIALIZED`), allowing the planner to push down WHERE predicates. Adding `AS MATERIALIZED` forces PostgreSQL to evaluate the CTE in isolation once and buffer results, acting as an intentional optimization fence.

    **Internal Mechanism:** When inlined, the planner merges the CTE subquery into the main query tree, enabling joint index selection. When `MATERIALIZED` is specified, the planner creates a `CTE Scan` plan node, isolating the subquery and preventing outer filter pushdown.

    **Common Mistake:** Using CTEs for code readability in complex reporting queries without realizing that `MATERIALIZED` CTEs prevent outer limit and index pushdowns, resulting in full table scans. [Concepts](/topics/database-sql/concepts.md#3-covering-partial-and-functional-indexes)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q25CteMaterializedOptimizationFenceExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 15. Why does `OFFSET` pagination fail at scale and how does Keyset pagination resolve it?

??? question "Reveal answer"
    `OFFSET N` requires the database to read and discard $N$ preceding rows off disk/cache, resulting in $O(N)$ linear latency degradation and data drift under concurrent inserts. Keyset pagination filters using indexed column criteria (`WHERE (created_at, id) < (:cursorCreatedAt, :cursorId)`), seeking directly to the next B-Tree leaf node in $O(\log N)$ time with constant $O(1)$ response time.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q15OffsetVsKeysetPagination.java"
        ```

### 16. How do Materialized Views work and how do you refresh them without locking?

??? question "Reveal answer"
    Standard views recompute SQL queries on every call, while Materialized Views store the query results physically on disk and support B-Tree indexing. Executing `REFRESH MATERIALIZED VIEW CONCURRENTLY` recalculates the view in the background while allowing concurrent `SELECT` queries, requiring a `UNIQUE` index on the materialized view.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q16MaterializedViews.java"
        ```

### 17. How does Table Partitioning improve query performance and maintenance?

??? question "Reveal answer"
    Table partitioning (by Range, List, or Hash) splits massive tables into independent physical child tables. **Partition Pruning** allows the query planner to bypass irrelevant partition tables entirely during query execution. Purging historical data is an instantaneous $O(1)$ metadata operation (`DROP TABLE partition_2020_01`), avoiding heavy WAL generation and dead tuple bloat.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q17TablePartitioning.java"
        ```

### 18. What is the Expand and Contract pattern for zero-downtime database migrations?

??? question "Reveal answer"
    A 4-phase migration pattern:
    1. **Expand:** Add the new column/table nullable without dropping the old one.
    2. **Backfill & Dual Write:** Deploy application version that writes to both columns and backfill historical rows.
    3. **Switch:** Deploy application reading from the new column.
    4. **Contract:** Drop old columns and database triggers once all legacy instances are retired.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q18ZeroDowntimeMigrations.java"
        ```

### 19. How do you calculate optimal database connection pool sizing (HikariCP formula)?

??? question "Reveal answer"
    Using the formula: $\text{connections} = ((\text{CPU Cores} \times 2) + \text{effective\_spindle\_count})$. For an 8-core server with SSD storage, an optimal pool size is $\approx 17$ connections. Oversizing connection pools causes CPU context switching thrashing, disk contention, and cache eviction, reducing overall system throughput.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q19ConnectionPoolSizing.java"
        ```

### 20. When should you use Common Table Expressions (CTEs) and Recursive Queries?

??? question "Reveal answer"
    Non-recursive CTEs (`WITH`) structure complex multi-step queries modularly and are inlined by PostgreSQL 12+ by default. Recursive CTEs (`WITH RECURSIVE`) enable graph traversal, hierarchical organizational trees, bill-of-materials resolution, and depth-first searches directly in SQL.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q20CommonTableExpressions.java"
        ```

### 26. How does Serializable Snapshot Isolation (SSI) detect Write Skew anomalies without pessimistic row locks?

??? question "Reveal answer"

    **Short Answer:** PostgreSQL Serializable Snapshot Isolation (SSI) detects rw-antidependencies using in-memory `SIREAD` predicate locks. When two concurrent transactions read disjoint sets of rows that satisfy a global invariant and subsequently update different rows, SSI detects a dangerous cycle in the dependency graph and aborts one transaction with SQLSTATE `40001`.

    **Deep Explanation:** In Repeatable Read, transactions view an immutable snapshot of committed data. Under the classic "on-call doctor" write skew scenario (invariant: $\ge 1$ doctor must be active), Doctor A and Doctor B both read `COUNT = 2` concurrently. Both withdraw, updating disjoint rows; both commit successfully, leaving 0 doctors. SSI tracks read dependencies without blocking concurrent readers or writers, failing fast with serialization failures on commit conflicts.

    **Internal Mechanism:** The lock manager registers `SIREAD` locks on tuples, pages, and relations. If a cycle of two consecutive rw-antidependency edges forms between concurrent transactions, PostgreSQL marks one transaction as `doomed` to break the cycle.

    **Example:** [Serializable Snapshot Isolation](/topics/database-sql/concepts.md#5-acid-properties-and-isolation-levels).

    **Common Mistake:** Assuming Repeatable Read prevents all concurrency anomalies; Repeatable Read prevents Phantom Reads and Non-Repeatable Reads, but permits Write Skew.

    **Production Consideration:** When running on `SERIALIZABLE` isolation level, the application layer MUST implement automated retry logic (with exponential backoff and jitter) to catch and replay `40001` serialization failure exceptions.

    **Follow-up Questions:**
    - How does optimistic locking in JPA compare to database-level SSI serialization retries? See [JPA / Hibernate: Optimistic vs Pessimistic Locking](/topics/jpa-hibernate/questions.md#15-how-do-optimistic-locking-and-pessimistic-locking-differ-in-jpa)
    - What differences exist between data races, race conditions, and isolation anomalies? See [Concurrency: Core Hazards](/topics/concurrency/questions.md#3-what-is-the-difference-between-a-race-condition-a-data-race-and-an-atomicity-violation)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q26SerializableSnapshotIsolationAnomalyExample.java"
        ```

### 27. How do you execute zero-downtime database schema refactoring using Expand and Contract with dual-writing views?

??? question "Reveal answer"

    **Short Answer:** Refactor schemas in five decoupled phases: (1) Expand by adding the new column/table as nullable, (2) Deploy application version $N+1$ dual-writing to both columns, (3) Backfill historical rows in small batches, (4) Deploy application version $N+2$ reading from the new column, and (5) Contract by dropping the legacy column.

    **Deep Explanation:** Direct schema changes like `ALTER TABLE users RENAME COLUMN email TO contact_email` acquire an `ACCESS EXCLUSIVE` lock and immediately break currently running instances of the application that expect the old column name, forcing system downtime. The Expand and Contract pattern decouples database migration from application code deployment.

    **Internal Mechanism:** Temporary updatable views or database triggers can be used to mirror writes between old and new columns during Phase 2 if multiple microservices share the underlying database.

    **Example:** [Expand and Contract migrations](/topics/database-sql/concepts.md#1-relational-modeling-and-normalization).

    **Common Mistake:** Backfilling all 100 million historical rows in a single monolithic `UPDATE` statement, generating gigabytes of WAL, holding locks, and saturating replication lag.

    **Production Consideration:** Backfill data using keyset-paginated batches with sleep intervals to minimize lock durations, and run schema validations during low-traffic maintenance windows.

    **Follow-up Questions:**
    - How do CI/CD pipelines automate zero-downtime Blue/Green and Canary database migrations? See [CI/CD: Deployment Strategies](/topics/ci-cd/questions.md)
    - How does evolutionary architecture guide schema changes in distributed domain boundaries? See [Architecture: Evolutionary Architecture](/topics/architecture/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q27ZeroDowntimeColumnMigrationExample.java"
        ```

### 28. How does Keyset seek pagination eliminate O(N) index scanning overhead compared to OFFSET pagination?

??? question "Reveal answer"

    **Short Answer:** `OFFSET N` forces the database engine to traverse, check MVCC visibility, and discard $N$ preceding rows ($O(N)$ execution cost), degrading query latency from milliseconds on page 1 to tens of seconds on page 10,000. Keyset pagination uses indexed tuple comparisons (`WHERE (created_at, id) < (:lastDate, :lastId)`), seeking directly to the next B-Tree leaf node in $O(\log N)$ time with $O(1)$ constant query time.

    **Deep Explanation:** In relational engines, `OFFSET` does not skip rows on disk; it evaluates all offset rows and throws them away. Additionally, concurrent inserts/deletes cause "page drift" where users see duplicate or missed records across pages. Keyset pagination relies on a composite index on `(created_at DESC, id DESC)`, enabling direct index range scans.

    **Internal Mechanism:** The query planner transforms tuple comparisons into a B-Tree search on the first index column, using the second column as a tie-breaker, scanning only the exact number of rows requested by `LIMIT`.

    **Example:** [Keyset pagination query planner cost](/topics/database-sql/concepts.md#6-pagination-patterns-offset-vs-keyset).

    **Common Mistake:** Attempting keyset pagination without a unique tie-breaker column (such as `id`), which causes pagination to skip rows when multiple rows share the exact same timestamp.

    **Production Consideration:** Use keyset pagination for infinite scroll mobile feeds and large public APIs; reserve `OFFSET` only for small administrative tables ($< 1,000$ rows) where random page jumps (`page=5`) are strictly required.

    **Follow-up Questions:**
    - Why does combining JOIN FETCH on collections with Spring Data Pageable trigger in-memory pagination? See [JPA / Hibernate: JOIN FETCH Pagination Hazard](/topics/jpa-hibernate/questions.md#27-why-is-combining-join-fetch-on-a-to-many-association-with-pagination-dangerous-and-how-do-you-redesign-it-using-two-phase-id-pagination-or-batch-fetching)
    - How should REST APIs format cursor tokens and pagination response metadata? See [REST API: Pagination Design](/topics/rest-api/questions.md#10-what-are-the-trade-offs-between-offset-based-and-keyset-cursor-pagination)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q28KeysetSeekPaginationCostExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### 21. How do you design a high-throughput distributed worker queue in PostgreSQL using `SKIP LOCKED`?

??? question "Reveal answer"
    Use a single atomic query with a CTE:
    ```sql
    WITH next_job AS (
        SELECT id FROM tasks 
        WHERE status = 'PENDING' AND scheduled_at <= NOW()
        ORDER BY priority DESC, id ASC LIMIT 1
        FOR UPDATE SKIP LOCKED
    )
    UPDATE tasks SET status = 'PROCESSING', started_at = NOW()
    FROM next_job WHERE tasks.id = next_job.id
    RETURNING tasks.*;
    ```
    This enables hundreds of concurrent worker threads to pull distinct jobs simultaneously with zero lock contention.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q21DistributedJobQueueSkipLocked.java"
        ```

### 22. How do you diagnose and remediate table and index bloat in production?

??? question "Reveal answer"
    Diagnose bloat by monitoring `n_dead_tup` vs `n_live_tup` in `pg_stat_user_tables` and using the `pgstattuple` extension. Rebuild bloated B-Tree indexes online using `REINDEX CONCURRENTLY`, and reclaim fragmented table disk space using `pg_repack` without taking exclusive table locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q22DatabaseBloatRemediation.java"
        ```

### 23. How do you migrate a 100-million row table to add a `NOT NULL` constraint without downtime?

??? question "Reveal answer"
    1. Add the column as nullable: `ALTER TABLE users ADD COLUMN age INT;` (Instant).
    2. Backfill existing rows in batches of 5,000 using keyset pagination to avoid long transactions.
    3. Add a check constraint without validating: `ALTER TABLE users ADD CONSTRAINT chk_age_not_null CHECK (age IS NOT NULL) NOT VALID;` (Instant).
    4. Validate constraint online: `ALTER TABLE users VALIDATE CONSTRAINT chk_age_not_null;` (Acquires `SHARE UPDATE EXCLUSIVE` lock, allowing concurrent reads and writes).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q23OnlineLargeTableMigration.java"
        ```

### 29. Production Incident: Autovacuum freeze starvation threatens database shutdown due to transaction ID wraparound

??? question "Reveal answer"

    **Short Answer:** PostgreSQL issued urgent alerts (`WARNING: database "mydb" must be vacuumed within 10000000 transactions; shutdown will occur in ... transactions`). Long-running analytics transactions and unmanaged abandoned replication slots blocked Autovacuum from advancing `relfrozenxid`, bringing the cluster close to emergency read-only shutdown.

    **Deep Explanation:** PostgreSQL uses 32-bit transaction IDs (XIDs) with circular modular comparison. If a table reaches $\approx 2.1$ billion transactions without freezing old tuples, old committed transactions would appear to be in the future, destroying ACID visibility. To prevent data corruption, PostgreSQL forcibly halts all write transactions when XID age reaches `autovacuum_freeze_max_age`.

    **Internal Mechanism:** An old transaction or stalled replication slot pins the cluster's global `xmin` horizon, preventing Autovacuum from cleaning dead tuples or freezing XIDs.

    **Example:** [Autovacuum wraparound risk](/topics/database-sql/code-review.md).

    **Common Mistake:** Terminating Autovacuum workers because they consume disk I/O, which only accelerates the timeline toward catastrophic database shutdown.

    **Production Consideration:** Terminate blocking queries via `pg_terminate_backend()`, drop inactive replication slots, run aggressive manual `VACUUM FREEZE ANALYZE`, and configure alerts on `pg_database.age(datfrozenxid) > 150_000_000`.

    **Follow-up Questions:**
    - How does container volume storage I/O throughput impact database vacuum performance? See [Docker: Storage and Volumes](/topics/docker/questions.md)
    - What Prometheus and Datadog metrics monitor PostgreSQL transaction age and dead tuple bloat? See [Observability: Database Metrics](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q29AutovacuumWraparoundScenarioExample.java"
        ```

### 30. Production Incident: Sudden query pool starvation caused by unindexed sequential scan on a 10-million row table

??? question "Reveal answer"

    **Short Answer:** A new search feature deployed to production lacked an index on a filtered column (`status`). Under peak traffic, 50 concurrent requests executed full table sequential scans on a 10-million row table, pinning database CPU at 100%, causing query duration to jump from 5ms to 45s, and exhausting the HikariCP connection pool across all microservices.

    **Deep Explanation:** In PostgreSQL, a query without a matching index scans every single heap page from disk into buffer cache. When 50 concurrent clients run this scan, disk I/O channels and CPU cores are completely saturated scanning dead and live tuples. Because queries take 45 seconds to finish, connections cannot return to HikariCP, causing connection acquisition timeouts across completely unrelated endpoints.

    **Internal Mechanism:** `EXPLAIN ANALYZE` shows `Seq Scan on orders (cost=0.00..328492.00 rows=100000 width=72) (actual time=45123.120..45230.450 rows=120)`.

    **Example:** [Sequential scan pool exhaustion](/topics/database-sql/code-review.md).

    **Common Mistake:** Sizing HikariCP connection pool larger to absorb the traffic spike; adding connections to a database with 100% CPU only increases lock contention and degrades performance further.

    **Production Consideration:** Terminate long-running sequential scans using `pg_cancel_backend()`, create the missing index online using `CREATE INDEX CONCURRENTLY` (which does not block table writes), and enforce `statement_timeout = '5s'` to prevent rogue queries from exhausting connection pools.

    **Follow-up Questions:**
    - How does the HikariCP pool sizing formula balance CPU cores against disk spindle concurrency? See [Spring Transactions: HikariCP Pool Sizing](/topics/spring-transactions/questions.md#8-how-should-hikaricp-connection-pools-be-sized-in-production)
    - How do slow query logs and distributed traces identify database bottlenecks in microservices? See [Performance: Database Query Profiling](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q30SeqScanPoolExhaustionScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
