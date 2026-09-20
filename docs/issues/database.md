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

## Related

- [Issue catalogue](index.md)
- [JPA / Hibernate Topics](../topics/jpa-hibernate/index.md)
