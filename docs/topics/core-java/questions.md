# Core Java Interview Questions

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between `==` and `equals`?

??? question "Reveal answer"

    **Short Answer:** For primitives, `==` compares values; for references it compares identity.
    `equals` compares logical equality when the type overrides it. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q01EqualsVsIdentityExample.java"
        ```

### 2. What contract connects `equals` and `hashCode`?

??? question "Reveal answer"

    **Short Answer:** Equal objects must have equal hashes. Equality must also be reflexive, symmetric,
    transitive and consistent. Unequal objects may collide. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q02EqualsHashCodeContractExample.java"
        ```

### 3. What problem do records solve?

??? question "Reveal answer"

    **Short Answer:** Records concisely declare transparent value carriers with final components and
    component-based equality. They are shallowly, not deeply, immutable. [Concept](/topics/core-java/concepts.md#immutability-and-java-21-data-types)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q03RecordImmutabilityExample.java"
        ```

### 4. Checked versus unchecked exceptions?

??? question "Reveal answer"

    **Short Answer:** Checked exceptions are enforced by the compiler; unchecked exceptions are not.
    Choose based on whether callers can meaningfully recover, not by habit. [Concept](/topics/core-java/concepts.md#exceptions-and-resources)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q04CheckedVsUncheckedExample.java"
        ```

### 5. What is Optional for?

??? question "Reveal answer"

    **Short Answer:** Primarily to make expected absence explicit in a return type. It is not a universal
    replacement for every nullable field or parameter. [Concept](/topics/core-java/concepts.md#optional)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q05OptionalUsageExample.java"
        ```

### 6. List, Set or Map?

??? question "Reveal answer"

    **Short Answer:** List models position and duplicates, Set uniqueness, and Map key-to-value
    association. Select by required semantics before implementation performance. [Concept](/topics/core-java/concepts.md#collections)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q06CollectionsHierarchyExample.java"
        ```

### 7. Intermediate versus terminal stream operation?

??? question "Reveal answer"

    **Short Answer:** Intermediate operations build a lazy pipeline; a terminal operation drives
    traversal and produces a result or side effect. [Concept](/topics/core-java/concepts.md#streams)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q08StreamLazinessExample.java"
        ```

### 8. What do `? extends T` and `? super T` mean?

??? question "Reveal answer"

    **Short Answer:** `extends` safely reads values as T from a producer; `super` safely writes T into a
    consumer—PECS. [Concept](/topics/core-java/concepts.md#generics-and-pecs)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q07GenericsPecsExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q09HashMapInternalsExample.java"
        ```

### 2. When does a collision chain treeify?

??? question "Reveal answer"

    **Short Answer:** Around eight nodes, but only when the table is at least 64 entries.

    **Internal Mechanism:** Below the minimum capacity HashMap prefers resize; trees can untreeify as they shrink.

    **Common Mistake:** Treating implementation constants as API guarantees. [Internals](/topics/core-java/internals.md#hashmap-lookup)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q10ArrayListVsLinkedListExample.java"
        ```

### 3. Why is a mutable HashMap key dangerous?

??? question "Reveal answer"

    **Short Answer:** Mutation can change its lookup bucket while the node remains in the insertion bucket.

    **Internal Mechanism:** HashMap never watches or re-indexes key state.

    **Common Mistake:** Replacing HashMap with ConcurrentHashMap; concurrency does not stabilize identity.
    [Review](/topics/core-java/code-review.md#mutable-map-key) · [Solution](/topics/core-java/solutions.md#immutable-map-identity)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q11ComparableVsComparatorExample.java"
        ```

### 4. What happens when ArrayList grows or inserts in the middle?

??? question "Reveal answer"

    **Short Answer:** Append is amortized O(1); growth copies O(n), and middle insertion shifts a suffix.

    **Internal Mechanism:** Elements are references in a contiguous array.

    **Common Mistake:** Choosing LinkedList for indexed or cache-sensitive workloads without measuring.
    [Internals](/topics/core-java/internals.md#arraylist-growth-and-iteration)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q12ShallowVsDeepImmutabilityExample.java"
        ```

### 5. Why should Comparator usually agree with equals?

??? question "Reveal answer"

    **Short Answer:** Sorted sets/maps treat `compare(a,b)==0` as the same key even if `equals` disagrees.

    **Internal Mechanism:** Tree navigation and uniqueness use comparison, not hashing.

    **Common Mistake:** Comparing only a non-unique display field. [Concept](/topics/core-java/concepts.md#equality-hashing-and-ordering)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q13ConcurrentHashMapExample.java"
        ```

### 6. What does type erasure remove?

??? question "Reveal answer"

    **Short Answer:** Most runtime type arguments are replaced by bounds; casts and bridge methods preserve source semantics.

    **Internal Mechanism:** Generic signatures may remain as metadata, but objects are not generally reified.

    **Common Mistake:** Expecting `instanceof List<String>` or `new T()`. [Internals](/topics/core-java/internals.md#type-erasure)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q14TypeErasureExample.java"
        ```

### 7. How do lazy and stateful stream operations differ?

??? question "Reveal answer"

    **Short Answer:** Laziness delays traversal; stateful operations such as `sorted` may buffer before emitting.

    **Internal Mechanism:** The terminal operation drives a sink chain; stateful stages form barriers.

    **Common Mistake:** Assuming each stage first creates a full collection. [Internals](/topics/core-java/internals.md#stream-execution)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q15StatefulStreamOperationsExample.java"
        ```

### 8. What happens if both the body and `close()` throw?

??? question "Reveal answer"

    **Short Answer:** The body exception remains primary and close failures are suppressed.

    **Internal Mechanism:** Compiler-generated nested close logic calls `addSuppressed` in reverse resource order.

    **Common Mistake:** Manual finally blocks that replace the original failure. [Internals](/topics/core-java/internals.md#try-with-resources-translation)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q16SuppressedExceptionsExample.java"
        ```

### 9. How do pattern matching for switch and record patterns enhance type safety and exhaustiveness?

??? question "Reveal answer"

    **Short Answer:** Pattern matching for switch eliminates unsafe explicit casting and enables compile-time exhaustiveness checking over sealed hierarchies, while record patterns deconstruct components directly with optional `when` guards.

    **Internal Mechanism:** The compiler verifies exhaustiveness over sealed type hierarchies without requiring an unnecessary `default` clause, and translates pattern matching via `invokedynamic` calling `TypeSwitch` bootstrap methods.

    **Common Mistake:** Adding an explicit `default` branch to a sealed switch statement, which silently disables compiler warnings when a new permitted subtype is added to the hierarchy. [Concepts](/topics/core-java/concepts.md#immutability-and-java-21-data-types)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q24PatternMatchingSwitchExample.java"
        ```

### 10. What contract do Sequenced Collections introduce and how do their reverse-order views work?

??? question "Reveal answer"

    **Short Answer:** Sequenced collections (`SequencedCollection`, `SequencedSet`, `SequencedMap`) provide a uniform contract for collections with defined encounter order (`getFirst()`, `getLast()`, `addFirst()`, `addLast()`) and lightweight `reversed()` views.

    **Internal Mechanism:** The `reversed()` method returns an un-copied reverse-ordered view backed directly by the original collection; mutations through either view reflect immediately in the other.

    **Common Mistake:** Manually creating reversed copies of lists or deques using iteration or `Collections.reverse()`, which incurs $O(N)$ allocation and breaks synchronization with the underlying source. [Concepts](/topics/core-java/concepts.md#collections)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q25SequencedCollectionsExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q17MutableKeyFailureExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q18EqualityContractEvolutionExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q19ParallelStreamStarvationExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q20OptionalMisuseExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q21ResourceSafetyAndAtomicityExample.java"
        ```

### 6. Legacy Java native serialization introduces remote code execution and invariant bypass risks. How do you safely eliminate it?

??? question "Reveal answer"

    **Short Answer:** Standard `java.io.Serializable` bypasses class constructors during deserialization, creating security vulnerabilities and invariant violations. Migrate to records or explicit formats (JSON, Protobuf).

    **Deep Explanation:** When an object is deserialized via `ObjectInputStream`, the JVM instantiates it using bytecode reflection without calling its constructor or validation logic. Malicious byte streams can instantiate corrupt domain states or trigger gadget chains leading to arbitrary code execution.

    **Internal Mechanism:** Records deserialize through their canonical constructor, guaranteeing that constructor preconditions, range checks, and defensive copies cannot be bypassed during deserialization.

    **Example:** [Record immutability](/topics/core-java/concepts.md#immutability-and-java-21-data-types).

    **Common Mistake:** Relying on `readObject()` to patch up broken invariants after unconstrained field assignment has already occurred.

    **Production Consideration:** Configure JVM serialization filters (`jdk.serialFilter`) to reject unvetted class names if legacy RMI or serialization must temporarily remain.

    **Follow-up Questions:**
    - How does class loading and bytecode verification interact with deserialization gadgets? See [JVM: Class Loading Lifecycle](/topics/jvm/questions.md#1-what-are-the-phases-of-the-class-loading-and-linking-lifecycle)
    - How does Spring Security protect against serialization vulnerabilities in session tokens? See [Spring Security: Session Management](/topics/spring-security/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q26SerializationSafetyExample.java"
        ```

### 7. A plugin system encounters ClassCastException and Metaspace leaks during dynamic reloading. Diagnose and redesign it.

??? question "Reveal answer"

    **Short Answer:** Class identity is defined by the pair `(FullyQualifiedName, DefiningClassLoader)`. Retaining references to loaded classes or their ClassLoader prevents Metaspace garbage collection and causes ClassCastException upon cast.

    **Deep Explanation:** A class is only eligible for unloading when its defining `ClassLoader` is unreachable. Static fields, `ThreadLocal`s, or callbacks registered into parent application loaders prevent the child loader from being collected, pinning its entire Metaspace allocation.

    **Internal Mechanism:** The JVM enforces that types loaded by separate `ClassLoader` instances are completely disjoint in the type system, even if compiled from byte-for-byte identical source code.

    **Example:** [ClassLoader isolation](/topics/core-java/concepts.md).

    **Common Mistake:** Closing a `URLClassLoader` without dereferencing all instances, threads, and thread-local variables created by that loader.

    **Production Consideration:** Isolate plugin communication through shared interfaces loaded strictly by the parent/bootstrap ClassLoader, and mandate explicit lifecycle teardown.

    **Follow-up Questions:**
    - What JVM flags track Metaspace memory allocations and class unloading events? See [JVM: Metaspace Internals](/topics/jvm/questions.md#4-what-is-metaspace-and-how-does-it-differ-from-permgen)
    - How does Spring Framework manage bean definitions dynamically across application contexts? See [Spring Core: IoC Container](/topics/spring-core/questions.md#1-what-is-inversion-of-control-ioc-and-how-does-dependency-injection-di-relate-to-it)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q27ClassLoaderIsolationExample.java"
        ```

### 8. High-frequency counter updates on multi-socket servers cause heavy CPU pipeline stalls. Diagnose and redesign it.

??? question "Reveal answer"

    **Short Answer:** Atomic variables (`AtomicLong`) or shared volatile fields suffer from CPU cache-line bouncing (false sharing) under heavy multi-threaded write contention; redesign using striped cell accumulators (`LongAdder`).

    **Deep Explanation:** Multi-core processors fetch memory in 64-byte cache lines. When multiple CPU cores repeatedly modify variables on the same or adjacent addresses, hardware cache coherency protocols (MESI) invalidate the entire cache line across all cores, saturating the interconnect bus.

    **Internal Mechanism:** `LongAdder` dynamically scales an internal array of padded `Cell`s (annotated with `@Contended`) based on thread contention, so each worker thread writes to an independent cache line.

    **Example:** [Concurrent data structures](/topics/core-java/concepts.md).

    **Common Mistake:** Using `AtomicLong.incrementAndGet()` in hot loops and assuming CAS hardware instructions scale linearly with CPU core counts.

    **Production Consideration:** Use `LongAdder` or `LongAccumulator` when throughput matters more than reading an instantaneous point-in-time exact sum; use `AtomicLong` only when strict monotonic sequencing is required.

    **Follow-up Questions:**
    - How does the Java Memory Model guarantee visibility of cell updates upon summation? See [Concurrency: Safe Publication](/topics/concurrency/questions.md#21-what-constitutes-safe-publication-of-shared-objects-in-the-java-memory-model)
    - How are hardware cache misses and IPC (instructions per cycle) diagnosed using profilers? See [Performance: Hardware Counters and Profiling](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q28FalseSharingPaddingExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q22DescriptorExhaustionScenarioExample.java"
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
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q23StreamConcurrencyBugScenarioExample.java"
        ```

### Scenario 3: Container killed by OS OOM-Killer despite low heap occupancy during high-throughput I/O

??? question "Reveal answer"

    **Short Answer:** Inspect off-heap native allocations (`DirectByteBuffer` or Netty byte buffers); off-heap allocations bypass JVM heap limits and trigger host cgroup memory kill (Exit 137).

    **Deep Explanation:** `ByteBuffer.allocateDirect()` allocates native memory via C `malloc`. The JVM tracks this via a lightweight heap reference associated with a `Cleaner` (PhantomReference). If heap pressure remains too low to trigger GC, old direct buffers are never unmapped, leading to silent native exhaustion.

    **Internal Mechanism:** Direct buffers only deallocate native memory when their corresponding Java phantom reference is cleared and enqueued during a garbage collection cycle.

    **Example:** [Direct memory leak scenario](/topics/core-java/code-review.md).

    **Common Mistake:** Tuning `-Xmx` higher, which actually worsens the problem because higher heap limits delay garbage collection of direct buffer phantom references.

    **Production Consideration:** Limit direct memory with `-XX:MaxDirectMemorySize` to force a synchronous JVM `OutOfMemoryError` before the Linux OOMKiller destroys the container, and use pooled direct buffers with deterministic release (`try-finally`).

    **Follow-up Questions:**
    - How does Netty manage reference-counted off-heap byte buffers in reactive applications? See [WebClient / WebFlux: Buffer Management](/topics/webclient-webflux/questions.md)
    - How do Native Memory Tracking (NMT) and jemalloc profiling isolate off-heap leaks? See [JVM: Memory Architecture and NMT](/topics/jvm/questions.md#3-how-is-jvm-memory-divided-between-stack-and-heap)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q29DirectMemoryLeakScenarioExample.java"
        ```

### Scenario 4: A scheduled background job stops executing without any error logged

??? question "Reveal answer"

    **Short Answer:** An unhandled `RuntimeException` or `Error` escaped from the runnable task passed to `ScheduledExecutorService`, permanently cancelling all future periodic executions.

    **Deep Explanation:** According to the `ScheduledExecutorService.scheduleAtFixedRate` contract, if any execution of the task encounters an unhandled exception, subsequent executions are suppressed and the returned `ScheduledFuture` transitions to cancelled/done.

    **Internal Mechanism:** The worker thread catches the uncaught exception, stores it in the `FutureTask` outcome state, and ceases re-queuing the task for future scheduled periods.

    **Example:** [Scheduled executor error suppression](/topics/core-java/code-review.md).

    **Common Mistake:** Assuming unhandled exceptions are logged to stdout/stderr or routed to `Thread.UncaughtExceptionHandler`.

    **Production Consideration:** Always wrap the entire body of scheduled runnable tasks in a top-level `try-catch (Throwable t)` block that logs failures and increments an error metric.

    **Follow-up Questions:**
    - How do Spring's `@Scheduled` and `TaskScheduler` handle uncaught exceptions? See [Spring Boot: Scheduling Internals](/topics/spring-boot/questions.md)
    - What thread pool sizing and rejection policies prevent executor queue starvation? See [Concurrency: ThreadPoolExecutor Configuration](/topics/concurrency/questions.md#14-what-are-the-core-parameters-of-threadpoolexecutor-and-how-do-its-4-rejection-policies-work)

    ??? example "Example"

        ```java
        --8<-- "modules/01-core-java/src/examples/java/lab/corejava/questions/Q30ScheduledExecutorFailureScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](/topics/core-java/concepts.md)
- [Internals](/topics/core-java/internals.md)
- [Code review](/topics/core-java/code-review.md)
