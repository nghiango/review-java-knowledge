# Core Java Interview Questions

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between `==` and `equals`?

??? question "Reveal answer"

    **Short Answer:** For primitives, `==` compares values; for references it compares identity.
    `equals` compares logical equality when the type overrides it. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 2. What contract connects `equals` and `hashCode`?

??? question "Reveal answer"

    **Short Answer:** Equal objects must have equal hashes. Equality must also be reflexive, symmetric,
    transitive and consistent. Unequal objects may collide. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 3. What problem do records solve?

??? question "Reveal answer"

    **Short Answer:** Records concisely declare transparent value carriers with final components and
    component-based equality. They are shallowly, not deeply, immutable. [Concept](/topics/core-java/concepts.md#immutability-and-java-21-data-types)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 4. Checked versus unchecked exceptions?

??? question "Reveal answer"

    **Short Answer:** Checked exceptions are enforced by the compiler; unchecked exceptions are not.
    Choose based on whether callers can meaningfully recover, not by habit. [Concept](/topics/core-java/concepts.md#exceptions-and-resources)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

### 5. What is Optional for?

??? question "Reveal answer"

    **Short Answer:** Primarily to make expected absence explicit in a return type. It is not a universal
    replacement for every nullable field or parameter. [Concept](/topics/core-java/concepts.md#optional)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

### 6. List, Set or Map?

??? question "Reveal answer"

    **Short Answer:** List models position and duplicates, Set uniqueness, and Map key-to-value
    association. Select by required semantics before implementation performance. [Concept](/topics/core-java/concepts.md#collections)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/CollectionExamples.java"
        ```

### 7. Intermediate versus terminal stream operation?

??? question "Reveal answer"

    **Short Answer:** Intermediate operations build a lazy pipeline; a terminal operation drives
    traversal and produces a result or side effect. [Concept](/topics/core-java/concepts.md#streams)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/StreamExamples.java"
        ```

### 8. What do `? extends T` and `? super T` mean?

??? question "Reveal answer"

    **Short Answer:** `extends` safely reads values as T from a producer; `super` safely writes T into a
    consumer—PECS. [Concept](/topics/core-java/concepts.md#generics-and-pecs)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/GenericExamples.java"
        ```

<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 1. How does HashMap find a key?

??? question "Reveal answer"

    **Short Answer:** It spreads `hashCode`, masks to a bucket, then checks hash and equality.

    **Internal Mechanism:** Buckets hold nodes as lists or trees; resize changes the mask and redistributes nodes.

    **Common Mistake:** Assuming hash equality means object equality. [Internals](/topics/core-java/internals.md#hashmap-lookup)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/CollectionExamples.java"
        ```

### 2. When does a collision chain treeify?

??? question "Reveal answer"

    **Short Answer:** Around eight nodes, but only when the table is at least 64 entries.

    **Internal Mechanism:** Below the minimum capacity HashMap prefers resize; trees can untreeify as they shrink.

    **Common Mistake:** Treating implementation constants as API guarantees. [Internals](/topics/core-java/internals.md#hashmap-lookup)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/CollectionExamples.java"
        ```

### 3. Why is a mutable HashMap key dangerous?

??? question "Reveal answer"

    **Short Answer:** Mutation can change its lookup bucket while the node remains in the insertion bucket.

    **Internal Mechanism:** HashMap never watches or re-indexes key state.

    **Common Mistake:** Replacing HashMap with ConcurrentHashMap; concurrency does not stabilize identity.
    [Review](/topics/core-java/code-review.md#mutable-map-key) · [Solution](/topics/core-java/solutions.md#immutable-map-identity)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 4. What happens when ArrayList grows or inserts in the middle?

??? question "Reveal answer"

    **Short Answer:** Append is amortized O(1); growth copies O(n), and middle insertion shifts a suffix.

    **Internal Mechanism:** Elements are references in a contiguous array.

    **Common Mistake:** Choosing LinkedList for indexed or cache-sensitive workloads without measuring.
    [Internals](/topics/core-java/internals.md#arraylist-growth-and-iteration)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/CollectionExamples.java"
        ```

### 5. Why should Comparator usually agree with equals?

??? question "Reveal answer"

    **Short Answer:** Sorted sets/maps treat `compare(a,b)==0` as the same key even if `equals` disagrees.

    **Internal Mechanism:** Tree navigation and uniqueness use comparison, not hashing.

    **Common Mistake:** Comparing only a non-unique display field. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/CollectionExamples.java"
        ```

### 6. What does type erasure remove?

??? question "Reveal answer"

    **Short Answer:** Most runtime type arguments are replaced by bounds; casts and bridge methods preserve source semantics.

    **Internal Mechanism:** Generic signatures may remain as metadata, but objects are not generally reified.

    **Common Mistake:** Expecting `instanceof List<String>` or `new T()`. [Internals](/topics/core-java/internals.md#type-erasure)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/GenericExamples.java"
        ```

### 7. How do lazy and stateful stream operations differ?

??? question "Reveal answer"

    **Short Answer:** Laziness delays traversal; stateful operations such as `sorted` may buffer before emitting.

    **Internal Mechanism:** The terminal operation drives a sink chain; stateful stages form barriers.

    **Common Mistake:** Assuming each stage first creates a full collection. [Internals](/topics/core-java/internals.md#stream-execution)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/StreamExamples.java"
        ```

### 8. What happens if both the body and `close()` throw?

??? question "Reveal answer"

    **Short Answer:** The body exception remains primary and close failures are suppressed.

    **Internal Mechanism:** Compiler-generated nested close logic calls `addSuppressed` in reverse resource order.

    **Common Mistake:** Manual finally blocks that replace the original failure. [Internals](/topics/core-java/internals.md#try-with-resources-translation)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 1. A cache intermittently misses keys that appear during iteration. Diagnose it.

??? question "Reveal answer"

    **Short Answer:** Check whether equality/hash fields mutate after insertion or disagree.

    **Deep Explanation:** The node remains in its insertion bucket while lookup computes a new bucket.

    **Internal Mechanism:** Hash spread and bucket masking precede `equals` checks.

    **Example:** [Mutable-key review](/topics/core-java/code-review.md#mutable-map-key).

    **Common Mistake:** Adding synchronization or switching to ConcurrentHashMap.

    **Production Consideration:** Use a final immutable identity type; migrate changed identity explicitly.

    **Follow-up Questions:** How would you detect orphaned entries? What equality policy fits JPA entities?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 2. Changing equality breaks deduplication after a release. How do you review it?

??? question "Reveal answer"

    **Short Answer:** Treat equality/order semantics as a persisted API contract and compare old/new fields.

    **Deep Explanation:** Sets, maps, caches and serialized dedup keys may all depend on the old identity domain.

    **Internal Mechanism:** Hash structures use both hash and equality; sorted structures use comparison equality.

    **Example:** [Equality solution](/topics/core-java/solutions.md#immutable-map-identity).

    **Common Mistake:** Updating `equals` without `hashCode`, Comparator, migrations or compatibility tests.

    **Production Consideration:** Version external keys and rehearse cache/data migration.

    **Follow-up Questions:** Can generated database IDs define equality before persistence?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/EqualityExamples.java"
        ```

### 3. A parallel stream calls a pricing API and throughput collapses. Redesign it.

??? question "Reveal answer"

    **Short Answer:** Remove blocking I/O from the common pool; use sequential work or an explicit bounded executor.

    **Deep Explanation:** More tasks do not create remote capacity; parked workers starve unrelated common-pool work.

    **Internal Mechanism:** Parallel streams split work into ForkJoin tasks and merge results.

    **Example:** [Stream review](/topics/core-java/code-review.md#stream-and-parallel-side-effects).

    **Common Mistake:** Raising common-pool parallelism globally.

    **Production Consideration:** Bound concurrency to downstream limits and retain input identity in failures.

    **Follow-up Questions:** When would virtual threads be preferable? How do you preserve order?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/StreamExamples.java"
        ```

### 4. Optional appears in DTOs, entity fields and method parameters. What is wrong?

??? question "Reveal answer"

    **Short Answer:** It creates ambiguous double-absence and framework friction without clarifying return absence.

    **Deep Explanation:** Required inputs should be validated; nullable transport fields need explicit schema semantics.

    **Internal Mechanism:** Optional is an ordinary value-based container, not language-level null safety.

    **Example:** [Optional review](/topics/core-java/code-review.md#optional-and-exception-misuse).

    **Common Mistake:** Replacing every null mechanically with Optional.

    **Production Consideration:** Define absence separately at domain, persistence and transport boundaries.

    **Follow-up Questions:** When is Optional as a parameter defensible?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

### 5. A CSV import leaks descriptors and leaves half the rows applied. Redesign it.

??? question "Reveal answer"

    **Short Answer:** Make resource ownership explicit, parse into an immutable result, then commit under an explicit policy.

    **Deep Explanation:** Closing and atomicity are separate concerns; try-with-resources fixes lifecycle, not partial writes.

    **Internal Mechanism:** Close failures become suppressed exceptions; caller-list mutation has no rollback.

    **Example:** [Resource review](/topics/core-java/code-review.md#resource-and-collection-mutation).

    **Common Mistake:** Adding only `finally { close(); }` and ignoring partial state.

    **Production Consideration:** Large imports may require transactional chunks, idempotency and checkpoints.

    **Follow-up Questions:** Who owns a Reader passed as an argument? How are rejected rows reported?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### Scenario 1: The process fails to open files after several hours

??? question "Reveal answer"

    **Short Answer:** Measure descriptors and correlate growth with import success/failure paths.

    **Deep Explanation:** Normal heap and CPU do not rule out exhaustion of an OS resource.

    **Internal Mechanism:** Unclosed readers retain file descriptors until deterministic close or eventual cleanup.

    **Example:** [Resource review](/topics/core-java/code-review.md#resource-and-collection-mutation).

    **Common Mistake:** Increasing the descriptor limit before fixing ownership.

    **Production Consideration:** Monitor open descriptors and test closure during read and close failures.

    **Follow-up Questions:** How are suppressed exceptions inspected?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/ResourceExamples.java"
        ```

### Scenario 2: Reports contain missing and reordered rows while CPU is low

??? question "Reveal answer"

    **Short Answer:** Inspect shared accumulators, blocking common-pool workers and the ordering contract.

    **Deep Explanation:** Low CPU can mean workers are parked on I/O while unsafe mutation loses results.

    **Internal Mechanism:** Parallel `forEach` does not serialize list appends or preserve side-effect order.

    **Example:** [Stream review](/topics/core-java/code-review.md#stream-and-parallel-side-effects).

    **Common Mistake:** Assuming parallel always increases throughput.

    **Production Consideration:** Use an explicit executor, downstream concurrency budget and cardinality/order metrics.

    **Follow-up Questions:** What changes if output may be unordered?

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/examples/StreamExamples.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](/topics/core-java/concepts.md)
- [Internals](/topics/core-java/internals.md)
- [Code review](/topics/core-java/code-review.md)
