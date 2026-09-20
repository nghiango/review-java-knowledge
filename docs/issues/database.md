# Database and JPA Issues

SQL, indexing, persistence-context, fetching, entity-modelling, and transaction failures.

## Entries

### N+1 Query Storm on Lazy Collection Traversal

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Iterating over a collection of managed parent entities and accessing a lazily loaded child association executes 1 initial query for parents plus $N$ individual child queries, causing severe database CPU saturation and latency spikes.

**Detection:** Query count assertions (`datasource-proxy`), SQL query logging, and APM tracing.

**Appears in:** [JPA / Hibernate — N+1 Queries](../topics/jpa-hibernate/code-review.md#1-n1-queries-from-lazy-iteration)

---

### Eager Fetching Cascades and Cartesian Product

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Declaring `FetchType.EAGER` across multiple entity associations forces Hibernate to execute sprawling multi-table joins or cascading single queries whenever an entity is loaded, causing excessive memory consumption and database load.

**Detection:** Hibernate SQL logs showing multi-table outer joins when loading single entities.

**Appears in:** [JPA / Hibernate — Eager Fetching](../topics/jpa-hibernate/code-review.md#2-eager-fetching-anti-pattern)

---

### Mutable `hashCode` on Generated Entity ID

**Type:** Correctness issue · **Severity:** High · **Difficulty:** Intermediate

Using an auto-generated primary key (`@Id`) in an entity's `hashCode()` calculation alters the object's hash code when the entity transitions from transient (ID is null) to managed (ID assigned by DB), corrupting `HashSet` and `HashMap` bucket lookups.

**Detection:** Unit tests checking `set.contains(entity)` before and after `em.persist()`.

**Appears in:** [JPA / Hibernate — Generated ID in equals/hashCode](../topics/jpa-hibernate/code-review.md#3-equals-and-hashcode-with-generated-id)

---

### Infinite JSON Recursion on Bidirectional Associations

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Basic

Directly returning managed JPA entities with bidirectional associations from REST controllers causes Jackson to recursively traverse parent-child references indefinitely, triggering `JsonMappingException: Infinite recursion` and `StackOverflowError`.

**Detection:** REST integration tests serializing bidirectional entities.

**Appears in:** [JPA / Hibernate — Bidirectional JSON Recursion](../topics/jpa-hibernate/code-review.md#4-bidirectional-json-recursion)

---

### Lazy Initialization Outside Active Session

**Type:** Correctness issue · **Severity:** High · **Difficulty:** Intermediate

Accessing uninitialized lazy associations on detached entities after the transaction and `PersistenceContext` have closed causes Hibernate to throw `LazyInitializationException: could not initialize proxy - no Session`.

**Detection:** Integration tests calling getters on uninitialized entity properties outside `@Transactional` boundaries.

**Appears in:** [JPA / Hibernate — Lazy Initialization Outside Transaction](../topics/jpa-hibernate/code-review.md#5-lazy-initialization-outside-transaction)

---

### Cascade Remove on Shared Many-to-Many Relationship

**Type:** Correctness issue · **Severity:** Critical · **Difficulty:** Senior

Applying `CascadeType.ALL` or `CascadeType.REMOVE` to `@ManyToMany` associations instructs Hibernate to delete shared target entities when a referencing entity is deleted, resulting in catastrophic data loss and foreign key constraint violations for unrelated records.

**Detection:** Integration tests asserting target entities remain in the database when unregistering a link.

**Appears in:** [JPA / Hibernate — Cascade ALL on ManyToMany](../topics/jpa-hibernate/code-review.md#6-cascade-all-on-manytomany-relationship)

---

### Direct JPA Entity Exposure in REST API

**Type:** Security issue · **Severity:** High · **Difficulty:** Intermediate

Accepting and returning JPA entities directly in `@RestController` endpoints enables Mass Assignment vulnerabilities (allowing untrusted clients to mutate privileged fields) and leaks sensitive internal attributes (like password hashes or internal audit fields).

**Detection:** Security static analysis and API contract validation tests.

**Appears in:** [JPA / Hibernate — Entity Exposed Through API](../topics/jpa-hibernate/code-review.md#7-entity-exposed-through-api)

---

### Unindexed Foreign Key Column

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Declaring a foreign key constraint without creating an explicit B-Tree index causes PostgreSQL to execute sequential table scans when joining or deleting parent records, acquiring table-level locks and triggering severe locking contention.

**Detection:** `EXPLAIN` query plans showing sequential scans on child tables and query logs during parent deletions.

**Appears in:** [Database / SQL — Unindexed Foreign Key](../topics/database-sql/code-review.md#1-unindexed-foreign-key-and-missing-index)

---

### Leftmost Prefix Rule Violation on Composite Index

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Placing low-cardinality or un-queried columns at the front of a composite index prevents direct B-Tree index traversal when queries filter on subsequent columns, forcing full index scans or table scans.

**Detection:** `EXPLAIN` query plans showing Bitmap Index Scan or Seq Scan instead of direct Index Scan.

**Appears in:** [Database / SQL — Wrong Composite Index Order](../topics/database-sql/code-review.md#2-wrong-composite-index-column-order)

---

### Deep Offset Pagination Latency Degradation

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Using `OFFSET N` requires reading, sorting, and discarding $N$ preceding rows off disk, causing linear $O(N)$ query slowdown on large tables and producing duplicate/missed rows during concurrent inserts.

**Detection:** Query latency increasing proportionally with page depth.

**Appears in:** [Database / SQL — Deep Offset Pagination](../topics/database-sql/code-review.md#3-deep-offset-pagination-performance)

---

### Lost Update Anomaly on Read-Modify-Write

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

Reading current balance or inventory into application memory and writing back without row-level locks or atomic updates allows concurrent transactions to overwrite each other's modifications.

**Detection:** Multi-threaded concurrency tests showing final state discrepancies.

**Appears in:** [Database / SQL — Lost Update Without Locking](../topics/database-sql/code-review.md#4-lost-update-without-locking)

---

### Prolonged Row Locks Across Remote I/O

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Senior

Holding exclusive database row locks (`SELECT FOR UPDATE`) across slow external network calls exhausts database connection pools and causes lock wait timeouts in concurrent requests.

**Detection:** APM traces showing long-running transactions and `pg_stat_activity` lock wait queues.

**Appears in:** [Database / SQL — Long Transaction Holding Locks](../topics/database-sql/code-review.md#5-long-transaction-holding-locks)

---

### SQL Injection via String Concatenation

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Basic

Assembling dynamic SQL queries by concatenating user inputs allows attackers to alter query semantics, extract unauthorized records, or execute destructive commands (OWASP A03).

**Detection:** Static security analysis and automated SQL injection vulnerability scanners.

**Appears in:** [Database / SQL — SQL Injection via String Concatenation](../topics/database-sql/code-review.md#6-sql-injection-via-string-concatenation)

---

### Destructive Schema Migration During Rolling Deployment

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Senior

Renaming or dropping columns in a single migration script immediately crashes active, in-flight V1 application instances during rolling zero-downtime deployments.

**Detection:** Deployment error spikes with `column does not exist` exceptions.

**Appears in:** [Database / SQL — Destructive Database Migration](../topics/database-sql/code-review.md#7-destructive-database-migration)

---

### Collation and constraint assumptions unverified by an embedded substitute

**Type:** Database issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** PostgreSQL, Spring Data JPA · **Interview frequency:** High · **Production impact:** High

An in-memory repository assumes a unique index on `email` is case-insensitive, that a duplicate insert
fails, and that `findAll()` returns insertion order. PostgreSQL's default `text` comparison is
case-sensitive, so two addresses differing only by case both persist, and no row order is guaranteed
without a `Sort`. Assert the collation and the constraint against the real engine instead of inferring
them from a map.

**Appears in:** `modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics`

---

## Related

- [Issue catalogue](index.md)
- [JPA / Hibernate Topics](../topics/jpa-hibernate/index.md)
- [Database / SQL Topics](../topics/database-sql/index.md)
