# JPA / Hibernate Interview Questions & Answers

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the architectural relationship between JPA and Hibernate?

??? question "Reveal answer"
    JPA (Jakarta Persistence) is a vendor-neutral Java specification defining standard ORM interfaces (`EntityManager`, `Query`, `EntityTransaction`) and annotations (`@Entity`, `@Table`). Hibernate is the reference ORM implementation that implements the JPA standard while offering proprietary enhancements like `Session`, `StatelessSession`, and multi-tenancy.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q01JpaVsHibernate.java"
        ```

### 2. What are the four entity lifecycle states in JPA and how do transitions occur?

??? question "Reveal answer"
    An entity instance exists in one of four states relative to the `PersistenceContext`:
    - **Transient:** Newly instantiated in memory using `new`, no database identity, not associated with an `EntityManager`.
    - **Managed:** Associated with an active `PersistenceContext`, tracked by dirty checking, changes automatically synchronized on flush.
    - **Detached:** Has a database identity, but its `EntityManager` session was closed, cleared, or detached.
    - **Removed:** Marked for deletion via `em.remove()`, SQL `DELETE` issued upon flush.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q02EntityLifecycleStates.java"
        ```

### 3. What are the differences between `@Id` generation strategies?

??? question "Reveal answer"
    - `IDENTITY`: Relies on database auto-increment columns (e.g. `BIGSERIAL`). Forces an immediate SQL `INSERT` on persist, disabling JDBC batching.
    - `SEQUENCE`: Utilizes database sequences. Allows Hibernate to pre-allocate blocks of IDs in memory (`allocationSize`), enabling full JDBC batching.
    - `TABLE`: Emulates sequences using a dedicated lock table (incurs high lock contention).
    - `AUTO`: Delegates generation strategy choice to the database dialect.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q03EntityMappingAnnotations.java"
        ```

### 4. Why is `EnumType.STRING` preferred over `EnumType.ORDINAL` for enum mappings?

??? question "Reveal answer"
    `EnumType.ORDINAL` persists enum integer positions (`0, 1, 2`). Adding new enum constants or reordering them silently corrupts existing database records. `EnumType.STRING` persists the constant name as `VARCHAR`, ensuring database readability and safe enum schema evolution.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q04ColumnAndEnumMapping.java"
        ```

### 5. How does `GenerationType.IDENTITY` affect Hibernate JDBC batching?

??? question "Reveal answer"
    `GenerationType.IDENTITY` requires executing an immediate SQL `INSERT` statement during `em.persist()` to retrieve the database-generated ID via JDBC `getGeneratedKeys()`. This bypasses Hibernate's action queue and disables write-behind JDBC batching. In contrast, `GenerationType.SEQUENCE` allocates IDs in memory ahead of time and batches all inserts during flush.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q05IdGenerationStrategies.java"
        ```

### 6. What are the default `FetchType` values for JPA associations?

??? question "Reveal answer"
    According to the JPA specification:
    - `@ManyToOne` and `@OneToOne` default to **`FetchType.EAGER`**.
    - `@OneToMany` and `@ManyToMany` default to **`FetchType.LAZY`**.
    
    Default `EAGER` on single-valued associations causes unnecessary joins and N+1 query storms. Production applications should explicitly set all associations to `FetchType.LAZY`.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q06FetchTypeDefaults.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 7. What is the difference between Hibernate First-Level and Second-Level Caches?

??? question "Reveal answer"
    - **First-Level (L1) Cache:** Session-scoped, non-thread-safe, always enabled. Functions as an identity map for managed entities loaded in the current transaction.
    - **Second-Level (L2) Cache:** `SessionFactory`-scoped, shared across sessions/transactions, optional (e.g. Ehcache, Infinispan). Stores disassembled hydrated state tuples rather than live managed entity instances.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q07FirstVsSecondLevelCache.java"
        ```

### 8. How does Hibernate's automatic dirty checking work?

??? question "Reveal answer"
    When an entity is loaded, Hibernate clones its property values into an internal snapshot array. During flush, Hibernate compares the entity's current in-memory fields with the snapshot array. If any field differs, an `UPDATE` statement is automatically generated and queued without requiring explicit `save()` or `update()` invocations.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q08DirtyCheckingMechanism.java"
        ```

### 9. What is the difference between `FlushModeType.AUTO` and `FlushModeType.COMMIT`?

??? question "Reveal answer"
    - `FlushModeType.AUTO` (default): Flushes pending mutations before queries whose tables overlap with dirty entities, and before transaction commit.
    - `FlushModeType.COMMIT`: Delays flushing until transaction commit. Queries executed during the transaction may return stale data if uncommitted changes are not manually flushed.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q09FlushModeTypeSemantics.java"
        ```

### 10. What causes the N+1 query problem and how is it resolved?

??? question "Reveal answer"
    Loading $N$ parent entities and accessing an uninitialized lazy association in a loop triggers 1 parent query followed by $N$ secondary queries. It is resolved using `JOIN FETCH` in JPQL, `@EntityGraph`, `@BatchSize`, or DTO projection queries.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q10NPlusOneProblem.java"
        ```

### 11. What is the difference between `@JoinColumn` and `mappedBy`?

??? question "Reveal answer"
    - `@JoinColumn`: Declares the **owning side** of the relationship, which physically controls and updates the foreign key column in SQL.
    - `mappedBy`: Declares the **inverse side**, which is a read-only mirror. Modifying the inverse collection alone without updating the owning side will not persist foreign key changes.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q11OwningSideAndMappedBy.java"
        ```

### 12. What is the difference between `CascadeType.REMOVE` and `orphanRemoval = true`?

??? question "Reveal answer"
    - `CascadeType.REMOVE`: Triggers a child delete only when the parent entity itself is deleted via `em.remove(parent)`.
    - `orphanRemoval = true`: Deletes child records when the parent is deleted **and** whenever a child is removed from the parent's collection (`parent.getChildren().remove(child)`).

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q12CascadeAndOrphanRemoval.java"
        ```

### 13. Why is using a generated `@Id` in `hashCode()` an anti-pattern?

??? question "Reveal answer"
    A transient entity has a `null` ID before persist. If added to a `HashSet` prior to database insertion, its hash code is computed based on `null` (0). When persisted, Hibernate assigns a generated ID, mutating the object's `hashCode()` and causing `set.contains()` or `set.remove()` to fail due to hash bucket mismatch.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q13EntityEqualsHashCode.java"
        ```

### 14. When should you choose JPQL, Criteria API, or Native SQL?

??? question "Reveal answer"
    - **JPQL:** Best for static, database-agnostic queries mapped to domain entities.
    - **Criteria API:** Best for dynamic query construction requiring compile-time type safety via the JPA metamodel.
    - **Native SQL:** Required for vendor-specific database capabilities (window functions, recursive CTEs, JSON operators) or bulk performance tuning.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q14JpqlVsCriteriaVsNative.java"
        ```

### 24. What is the execution sequence of Hibernate ActionQueue during flush and how can it cause constraint violations?

??? question "Reveal answer"

    **Short Answer:** Hibernate flushes statements in strict ActionQueue order: Inserts $\to$ Updates $\to$ Collection removals $\to$ Collection updates $\to$ Collection recreations $\to$ Deletes. Because Inserts run before Deletes, replacing an entity with the same unique key in a single flush causes a Unique Key Constraint violation.

    **Internal Mechanism:** The `ActionQueue` sorts SQL executions by action class to maximize JDBC batching efficiency. If `em.remove(oldRecord)` is called followed by `em.persist(newRecord)` with the same unique business key, Hibernate queues the `InsertAction` before the `DeleteAction`.

    **Common Mistake:** Relying on Java statement order inside `@Transactional` methods and expecting deletes to execute before subsequent inserts without calling `em.flush()`. [Concepts](/topics/jpa-hibernate/concepts.md#6-flushmodetype-semantics)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q24HibernateActionQueueOrderingExample.java"
        ```

### 25. How are Second-Level Cache regions partitioned and why does the Query Cache invalidate frequently?

??? question "Reveal answer"

    **Short Answer:** The Second-Level Cache partitions into Entity (dehydrated fields), Collection (foreign key arrays), NaturalId, and Query regions. The Query Cache stores query parameter hashes mapping to matching `@Id` lists, but any `INSERT`, `UPDATE`, or `DELETE` on the entity table invalidates all cached queries for that table.

    **Internal Mechanism:** The `UpdateTimestampsCache` records the timestamp of the most recent write to any entity table. When reading from the query cache, Hibernate checks if `cacheTimestamp < tableUpdateTimestamp`; if so, the query cache entry is evicted as stale.

    **Common Mistake:** Enabling Query Cache on frequently updated transactional tables, incurring severe cache thrashing and lock contention without read latency improvements. [Concepts](/topics/jpa-hibernate/concepts.md#1-entity-lifecycle-states)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q25SecondLevelCacheRegionsExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 15. How do Optimistic Locking and Pessimistic Locking differ in JPA?

??? question "Reveal answer"
    - **Optimistic Locking:** Uses `@Version` to verify at update time that the row has not been modified by another transaction. Throws `OptimisticLockException` on conflict. Ideal for read-heavy, low-contention systems.
    - **Pessimistic Locking:** Issues database row locks (`SELECT ... FOR UPDATE` via `LockModeType.PESSIMISTIC_WRITE`) to block concurrent transactions until the current transaction commits. Ideal for high-contention workflows.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q15OptimisticVsPessimisticLocking.java"
        ```

### 16. Why is Open Session in View (OSIV) disabled in high-throughput production systems?

??? question "Reveal answer"
    OSIV binds the `EntityManager` and its underlying database connection to the entire HTTP request lifecycle. Slow template rendering or third-party HTTP calls keep database connections held open, rapidly exhausting the HikariCP connection pool. Disabling OSIV ensures connections are returned to the pool immediately upon service transaction completion.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q16OpenSessionInViewPitfalls.java"
        ```

### 17. What are the trade-offs of using `@DynamicUpdate`?

??? question "Reveal answer"
    By default, Hibernate uses precompiled static `UPDATE` statements containing all columns, maximizing database statement cache hit ratios. `@DynamicUpdate` generates dynamic SQL containing only dirty columns at runtime. While this reduces network payload size, it incurs CPU overhead calculating dynamic SQL strings and prevents database prepared statement reuse.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q17DynamicUpdateAndBytecodeEnhancement.java"
        ```

### 18. How do you configure and optimize Hibernate JDBC batching?

??? question "Reveal answer"
    Configure `hibernate.jdbc.batch_size` (e.g. 50), `hibernate.order_inserts=true`, and `hibernate.order_updates=true`. Ensure entity primary keys use `GenerationType.SEQUENCE` with `allocationSize` matching the batch size, avoiding `IDENTITY` generation which interrupts the batch queue.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q18HibernateJdbcBatching.java"
        ```

### 19. Compare JPA inheritance strategies (`SINGLE_TABLE`, `JOINED`, `TABLE_PER_CLASS`).

??? question "Reveal answer"
    - `SINGLE_TABLE` (default): Fast queries without joins, but all subclass columns must be nullable.
    - `JOINED`: Fully normalized schema supporting `NOT NULL` constraints, but polymorphic queries require complex outer joins across tables.
    - `TABLE_PER_CLASS`: Independent tables per concrete entity, but polymorphic queries require expensive `UNION ALL` statements.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q19InheritanceMappingStrategies.java"
        ```

### 20. How do you process large datasets without exhausting JVM heap memory?

??? question "Reveal answer"
    Standard `EntityManager` queries load all managed entities into the L1 cache, leading to `OutOfMemoryError`. To process millions of records safely, use Hibernate's `StatelessSession` (which bypasses L1 cache, dirty checking, and cascades) or execute paginated batches with periodic `em.flush()` and `em.clear()`.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q20LargeDatasetProcessing.java"
        ```

### 26. Why does accessing lazy associations fail outside transactions when OSIV is disabled, and how do you design read-only queries with DTO projections or EntityGraphs?

??? question "Reveal answer"

    **Short Answer:** With Open Session in View disabled (`spring.jpa.open-in-view: false`), the `EntityManager` closes when the service transaction commits. Entities become detached; accessing lazy associations in the controller or serialization layer throws `LazyInitializationException`. Adding `@Transactional` only to `service.getOrder()` does not fix it because the transaction ends before the controller accesses the items.

    **Deep Explanation:** In Spring applications with OSIV disabled, persistence context lifetime matches transaction boundary. When an entity is returned from a transactional service method, its session closes and database connection returns to the pool. When `order.getItems()` is accessed afterwards, Hibernate has no open session to execute the lazy SQL query.

    **Internal Mechanism:** Bytecode-enhanced or CGLIB entity proxies check `session.isOpen()`; if null or disconnected, the proxy throws `LazyInitializationException: could not initialize proxy - no Session`.

    **Example:** [OSIV disabled lazy loading](/topics/jpa-hibernate/concepts.md#1-entity-lifecycle-states).

    **Common Mistake:** Enabling OSIV (`spring.jpa.open-in-view: true`) as a quick workaround, which holds database connections hostage during view rendering and causes connection pool starvation.

    **Production Consideration:** For read-only endpoints, perform DTO mapping inside the transactional service boundary using `JOIN FETCH` / `@EntityGraph`, or query constructor DTO expressions directly (`SELECT new com.example.OrderResponse(...)`) to bypass entity state loading completely.

    **Follow-up Questions:**
    - How does transaction boundary scoping interact with HikariCP connection borrowing? See [Spring Transactions: Proxy Mechanism](/topics/spring-transactions/questions.md#1-how-does-springs-transactional-annotation-work-under-the-hood-and-why-does-self-invocation-bypass-it)
    - How do DTO projections prevent Mass Assignment and serialization recursion? See [REST API: DTO Validation](/topics/rest-api/questions.md#16-what-is-mass-assignment-vulnerability-cwe-915-and-how-do-dtos-prevent-it)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q26OsivDisabledLazyLoadingExample.java"
        ```

### 27. Why is combining JOIN FETCH on a to-many association with pagination dangerous, and how do you redesign it using two-phase ID pagination or batch fetching?

??? question "Reveal answer"

    **Short Answer:** A `JOIN FETCH` on `@OneToMany` collection multiplies each root row into multiple SQL join rows. Applying database `LIMIT` and `OFFSET` directly would slice across child items instead of distinct parent entities. Hibernate prevents incorrect results by in-memory pagination with warning `HHH000104`, loading all rows into heap memory and risking `OutOfMemoryError`.

    **Deep Explanation:** If there are 100 orders, each with 10 items, joining produces 1,000 SQL rows. If `Pageable` requests page size 20, database `LIMIT 20` would return only 2 complete orders (20 item rows), completely corrupting pagination counts. Hibernate detects `firstResult`/`maxResults` on a collection fetch, emits `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!`, and fetches all 1,000 rows into RAM.

    **Internal Mechanism:** SQL row multiplicity prevents mapping SQL limits to entity counts without subqueries.

    **Example:** [Join fetch pagination hazard](/topics/jpa-hibernate/concepts.md#3-association-mappings-and-fetch-defaults).

    **Common Mistake:** Ignoring `HHH000104` warnings in development with small test datasets, only to encounter catastrophic JVM heap exhaustion when table volume grows in production.

    **Production Consideration:** Use **Two-Phase ID Pagination**: first query a page of parent IDs (`SELECT o.id FROM Order o ORDER BY o.id DESC`), then fetch orders and items via `WHERE o.id IN (:ids)`. Alternatively, use `@BatchSize(size = 20)` or `hibernate.default_batch_fetch_size: 20` to eliminate N+1 queries while preserving standard pagination.

    **Follow-up Questions:**
    - How does Keyset pagination optimize large-scale pagination over OFFSET pagination? See [Database / SQL: Keyset Pagination](/topics/database-sql/questions.md#15-why-does-offset-pagination-fail-at-scale-and-how-does-keyset-pagination-resolve-it)
    - How does Hibernate batch fetching group collection loads across multiple entities? See [JPA / Hibernate: N+1 Problem](/topics/jpa-hibernate/questions.md#10-what-causes-the-n1-query-problem-and-how-is-it-resolved)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q27JoinFetchPaginationHazardExample.java"
        ```

### 28. How does Hibernate StatelessSession process large datasets without First-Level Cache overhead or dirty checking?

??? question "Reveal answer"

    **Short Answer:** `StatelessSession` provides a command-oriented API that bypasses the First-Level Cache (persistence context), automatic dirty checking, cascading, and the Second-Level Cache, streaming database rows directly at native JDBC speeds.

    **Deep Explanation:** In standard `Session`, every loaded entity is tracked in the First-Level Cache identity map. For millions of rows, memory consumption explodes unless manually flushed and cleared. `StatelessSession` does not maintain an identity map or snapshot entity state; calling `session.update()` immediately issues raw SQL `UPDATE` without dirty checking.

    **Internal Mechanism:** `StatelessSessionImpl` bypasses the persistence context action queue and manages JDBC statements directly through `ConnectionProvider`.

    **Example:** [StatelessSession batch processing](/topics/jpa-hibernate/concepts.md#1-entity-lifecycle-states).

    **Common Mistake:** Expecting `@OneToMany` cascades or automatic dirty checking to work inside `StatelessSession`; all operations must be invoked explicitly.

    **Production Consideration:** Use `StatelessSession` for batch ETL pipelines, data migrations, and large analytical export jobs where ORM persistence context overhead provides no value.

    **Follow-up Questions:**
    - How do CTEs and recursive SQL queries process hierarchical datasets directly in the database engine? See [Database / SQL: CTEs and Hierarchical Queries](/topics/database-sql/questions.md#20-when-should-you-use-common-table-expressions-ctes-and-recursive-queries)
    - What JVM GC tuning and heap sizing strategies prevent memory fragmentation during large batch streaming? See [JVM: Memory Layout and Sizing](/topics/jvm/questions.md#3-how-is-jvm-memory-divided-between-stack-and-heap)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q28StatelessSessionBatchStreamingExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### 21. How do you handle high-concurrency inventory reservation without overselling?

??? question "Reveal answer"
    Under high-contention flash sales, optimistic locking causes severe retry storms. The recommended production pattern is an atomic database-level SQL update:
    ```sql
    UPDATE inventory SET stock = stock - :qty WHERE id = :id AND stock >= :qty
    ```
    If row-level locking is necessary, use `LockModeType.PESSIMISTIC_WRITE` with a short timeout.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q21ConcurrentInventoryReservation.java"
        ```

### 22. How do you diagnose and prevent memory leaks in batch processing jobs?

??? question "Reveal answer"
    In long-running batch jobs, entities accumulated in the First-Level cache are never garbage collected until session closure. Diagnosing involves analyzing heap dumps for large `StatefulPersistenceContext` maps. Prevention requires clearing the session (`em.clear()`) after batch flushes or utilizing `StatelessSession`.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q22BatchMemoryLeakPrevention.java"
        ```

### 23. How do you resolve `MultipleBagFetchException` when migrating legacy queries?

??? question "Reveal answer"
    Hibernate throws `MultipleBagFetchException` when a query attempts to `JOIN FETCH` two or more `List` collections simultaneously, as this generates a Cartesian product. Resolve this by changing one collection type to `Set`, or preferably, splitting the retrieval into sequential queries using `@BatchSize` or separate entity graph queries.

    ??? example "Example"
        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q23MultipleBagFetchExceptionMigration.java"
        ```

### 29. Production Incident: Batch processing job crashes with OutOfMemoryError due to un-cleared PersistenceContext

??? question "Reveal answer"

    **Short Answer:** A nighttime batch processing job iterating over 500,000 database records in a single `@Transactional` method caused JVM heap memory exhaustion (`OutOfMemoryError: Java heap space`) because all loaded entities remained permanently managed in the First-Level Cache.

    **Deep Explanation:** In JPA, the `EntityManager` acts as an identity map. Every entity read during a transaction stays in memory until the transaction terminates so Hibernate can perform dirty checking. Reading 500,000 entities retains 500,000 object graphs in RAM simultaneously.

    **Internal Mechanism:** `StatefulPersistenceContext.entitiesByKey` holds strong references to all managed entities and their snapshot hydration arrays.

    **Example:** [PersistenceContext batch memory leak](/topics/jpa-hibernate/code-review.md).

    **Common Mistake:** Sizing JVM heap (`-Xmx`) larger, which merely postpones the OOM crash and increases Stop-The-World garbage collection pause times.

    **Production Consideration:** Implement chunk-based processing with periodic `em.flush()` (sending pending SQL writes to the socket) followed immediately by `em.clear()` (evicting all managed entities from the 1st-level cache), or execute the batch job via `StatelessSession`.

    **Follow-up Questions:**
    - How do heap dumps and memory profiling tools identify large PersistenceContext object graphs? See [JVM: Memory Error Taxonomy](/topics/jvm/questions.md#8-what-are-the-different-types-and-causes-of-outofmemoryerror)
    - How does Spring Batch implement chunk-oriented processing with commit intervals? See [Spring Boot: Starters and Batch](/topics/spring-boot/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q29PersistenceContextBatchLeakScenarioExample.java"
        ```

### 30. Production Incident: Flash sale concurrent orders oversell inventory due to un-synchronized check-then-act queries

??? question "Reveal answer"

    **Short Answer:** During a flash sale with 1 remaining item, two concurrent user checkout requests read `stock = 1` simultaneously using standard `SELECT`; both verified stock availability, and both issued `UPDATE item SET stock = stock - 1`, resulting in `stock = -1` (inventory oversold!).

    **Deep Explanation:** Standard relational queries execute under Read Committed isolation, which does not prevent non-repeatable reads or concurrent phantom updates across transactions without row-level locking. Under high concurrency, reading before updating is an anti-pattern known as "check-then-act race condition".

    **Internal Mechanism:** Without explicit locking, transaction A and transaction B both read the committed row snapshot concurrently; both pass validation and write updates.

    **Example:** [Concurrent inventory reservation](/topics/jpa-hibernate/code-review.md).

    **Common Mistake:** Wrapping the Java method in `synchronized`, which fails across multiple application instances and Kubernetes pods behind a load balancer.

    **Production Consideration:** Use atomic conditional SQL:
    ```sql
    UPDATE item SET stock = stock - :qty WHERE id = :id AND stock >= :qty
    ```
    If updating entity aggregates, use `@Version` optimistic locking (retrying on `OptimisticLockException`) or `LockModeType.PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`).

    **Follow-up Questions:**
    - What differences exist between `SELECT FOR UPDATE`, `NOWAIT`, and `SKIP LOCKED` in PostgreSQL? See [Database / SQL: Locking Hierarchy](/topics/database-sql/questions.md#13-what-is-the-difference-between-select-for-update-nowait-and-skip-locked)
    - How do atomic CAS primitives prevent race conditions in in-memory concurrency? See [Concurrency: Atomic Variables and CAS](/topics/concurrency/questions.md#10-how-do-atomic-variables-and-compare-and-swap-cas-work-and-how-is-the-aba-problem-prevented)

    ??? example "Example"

        ```java
        --8<-- "modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q30InventoryOversellingScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
