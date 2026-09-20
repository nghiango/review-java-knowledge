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
<!-- --8<-- [end:scenarios] -->
