# Core Java — Practice Q&A

Senior-depth practice from the supplied interview-prep case study (§2 and §17).
Answers are collapsed: give the verdict first, then defend the mechanism and trade-offs.
HyperScale Commerce names and measurements describe the supplied case study, not verified lab implementations.
Examples and deeper explanations live in [Concepts](concepts.md), [Internals](internals.md),
[Code Review](code-review.md), and [Solutions](solutions.md).

### Q1. How do the four OOP pillars protect business invariants?

??? question "Reveal answer"
    - **Encapsulate valid state and expose behavior through stable contracts.**
    - **Mechanism:** constructors and domain operations enforce invariants; interfaces separate callers from implementation. Dynamic dispatch selects overridden instance behavior.
    - **Failure:** public setters permit invalid intermediate states; inheritance can expose parent assumptions.
    - **Fix:** validate value objects, keep mutation behind domain methods, and prefer composition for variable behavior.
    - **Production:** test invalid transitions, not getter coverage. Avoid splitting one invariant across services without a consistency protocol.
    - **Follow-up:** Which rules belong in database constraints as well as domain code?
    - **Takeaway:** Encapsulation protects rules, not merely fields.

### Q2. When is composition safer than inheritance?

??? question "Reveal answer"
    - **Prefer delegation unless a stable subtype genuinely satisfies its parent's contract.**
    - **Mechanism:** subclasses inherit implementation assumptions; decorators depend on a narrower interface and can combine behaviors.
    - **Failure:** an overridden method changes behavior relied upon by another parent method. Test substitutability, including exceptions and preconditions.
    - **Fix:** inject collaborators; keep framework inheritance narrow and intentional. A filter base class can be appropriate without making the whole domain hierarchical.
    - **Trade-off:** composition adds forwarding and wiring; inheritance can express a deliberately designed template method succinctly.
    - **Follow-up:** Would a parent upgrade break subclasses without a compilation error?
    - **Takeaway:** Reuse behavior without inheriting accidental obligations.

### Q3. Interface, abstract class, or sealed hierarchy: how do you choose?

??? question "Reveal answer"
    - **Use interfaces for open contracts, abstract classes for shared implementation, sealed types for closed alternatives.**
    - **Mechanism:** interfaces permit multiple implemented capabilities; abstract classes supply instance state and constructors but consume the single superclass slot.
    - **Failure:** adding abstract interface methods breaks implementers; broad default methods can hide incompatible semantics.
    - **Fix:** evolve contracts deliberately. Use sealed results when the owner controls all variants and exhaustive handling matters.
    - **Trade-off:** sealing improves exhaustiveness but restricts third-party extension and requires coordinated evolution.
    - **Follow-up:** How do independently deployed binaries behave when a new subtype appears?
    - **Takeaway:** Choose openness explicitly, not by habit.

### Q4. What makes a functional interface, and when are primitive specializations useful?

??? question "Reveal answer"
    - **A functional interface has one abstract method after inheritance and override rules are applied.**
    - **Mechanism:** lambdas and method references target that contract. `Predicate`, `Function`, `Consumer`, and `Supplier` model tests, transformations, effects, and deferred values.
    - **Failure:** boxing in hot pipelines adds allocation; a captured mutable object remains mutable despite an effectively-final reference.
    - **Fix:** use `IntPredicate` or `ToLongFunction` where profiling supports it; make captured state immutable or synchronized.
    - **Trade-off:** domain-specific interfaces explain meaning better than deeply nested generic functions, but add types.
    - **Follow-up:** Why does capturing `this` retain the enclosing object?
    - **Takeaway:** A function contract says nothing by itself about purity or thread safety.

### Q5. When does a stream execute, and when is a loop the better choice?

??? question "Reveal answer"
    - **Streams execute lazily at terminal operations; choose loops for explicit control and complex effects.**
    - **Mechanism:** stateless stages can fuse; stateful operations such as sorting buffer data. Short-circuiting can avoid processing the entire input.
    - **Failure:** side effects may be skipped by optimizations, reordered in parallel execution, or partially applied before failure.
    - **Fix:** keep transformations pure, close resource-backed streams, and use loops for retries or transactional workflows.
    - **Production:** measure allocation and blocking with JFR; parallel streams are not a default server-side I/O executor.
    - **Follow-up:** Why might `peek` not run before `count`?
    - **Takeaway:** Use streams to describe transformations, not to disguise workflows.

### Q6. What do `final`, `volatile`, `synchronized`, `transient`, `native`, and `static` actually guarantee?

Also relevant: [Concurrency](../concurrency/index.md)

??? question "Reveal answer"
    - **These keywords govern different contracts; none means general-purpose thread safety.**
    - **Mechanism:** `final` prevents reassignment; `volatile` establishes visibility/order; `synchronized` adds mutual exclusion. `static` is class-associated state, per defining class loader.
    - **Failure:** `final List` can mutate; `volatile count++` races; static maps retain objects. Final-field initialization safety requires correct construction without `this` escape.
    - **Fix:** protect compound invariants with locks or atomic state, and publish references through a defined concurrency boundary.
    - **Nuance:** `transient` affects default Java serialization, not all JSON mapping. `native` declares a native implementation; FFM does not require that modifier.
    - **Follow-up:** Is an immutable object's publication equivalent to a volatile write?
    - **Takeaway:** Identify the exact guarantee before choosing the keyword.

### Q7. Why does `ArrayList` usually outperform `LinkedList`?

??? question "Reveal answer"
    - **Contiguous storage and low allocation usually beat pointer-based insertion advantages.**
    - **Mechanism:** array lookup is constant time; append is amortized constant time; middle edits shift elements. Linked-list indexing traverses nodes.
    - **Failure:** claiming constant-time linked-list insertion ignores finding the position. Node allocation and pointer chasing increase GC and cache misses.
    - **Fix:** default to `ArrayList`; use `ArrayDeque` for queues and stacks. Pre-size when bounds are known without overallocating enormous buffers.
    - **Production:** benchmark realistic iteration/edit patterns and retained memory; growth factors and object sizes are implementation details.
    - **Follow-up:** Does retaining a positioned list iterator change the trade-off?
    - **Takeaway:** Include locality and allocation in complexity reasoning.

### Q8. How do hashing, equality, collision handling, and resizing affect `HashMap` correctness?

Canonical: [Equality contract](questions.md#2-what-contract-connects-equals-and-hashcode)

??? question "Reveal answer"
    - **Stable, compatible `equals` and `hashCode` are prerequisites for reliable lookup.**
    - **Mechanism:** a spread hash selects a power-of-two bucket; equality resolves collisions. OpenJDK treeifies crowded bins when capacity permits; resize redistributes entries.
    - **Failure:** mutated key state leaves entries in old buckets. Equal objects with different hashes may never be compared. Records are only shallowly immutable.
    - **Fix:** use stable value keys, defensive copies, and contract tests for equality, hashing, and mutation. Never use concurrent writes to a plain `HashMap`.
    - **Trade-off:** tree bins mitigate collisions but are not a universal logarithmic guarantee for every adversarial, non-comparable key workload; thresholds are implementation details.
    - **Follow-up:** Why can iteration find a key that `get` misses?
    - **Takeaway:** Hashing finds candidates; equality establishes identity.

### Q9. Why can removal during for-each iteration fail?

??? question "Reveal answer"
    - **Structural mutation outside the iterator can invalidate its traversal contract.**
    - **Mechanism:** fail-fast iterators compare structural modification counters on selected operations. This is best-effort detection, not synchronization.
    - **Failure:** some modifications escape immediate detection; an unmodifiable collection instead throws on mutation. A concurrent collection has different traversal guarantees.
    - **Fix:** use `Iterator.remove`, `removeIf`, or build a filtered result under appropriate ownership. Do not catch `ConcurrentModificationException` as a concurrency strategy.
    - **Trade-off:** copy-on-write suits read-heavy, small collections but copies on writes; weakly consistent traversal does not provide a snapshot.
    - **Follow-up:** Is changing an element's fields a structural modification?
    - **Takeaway:** Pick traversal semantics before choosing the collection.

### Q10. Checked versus unchecked exceptions: where should translation happen?

Also relevant: [Spring Transactions](../spring-transactions/index.md)

??? question "Reveal answer"
    - **Translate errors at meaningful boundaries while preserving causes and recovery semantics.**
    - **Mechanism:** checked exceptions require handling or declaration; unchecked exceptions do not. Neither category automatically means recoverable or unrecoverable.
    - **Failure:** swallowing an exception converts failure into apparent success; indiscriminate wrapping destroys classification and rollback intent.
    - **Fix:** expose domain errors at application boundaries, retain causes, and configure rollback explicitly where checked failures must abort writes.
    - **Production:** classify validation, conflicts, transient dependency failures, and programming defects separately; log once with safe context.
    - **Follow-up:** Can a caught persistence failure still leave a transaction rollback-only?
    - **Takeaway:** Exception types are part of the behavioral contract.

### Q11. What do records and modern pattern matching improve, and what do they not solve?

Merged from: §2 modern Java + §17.11

??? question "Reveal answer"
    - **Records reduce value boilerplate; sealed types and patterns make closed decisions explicit.**
    - **Mechanism:** record components generate accessors and value methods. Compact constructors validate or copy components; Java 21 switches can exhaustively cover sealed alternatives.
    - **Failure:** mutable components undermine immutability; generated `toString` can reveal sensitive fields. Existing compiled clients do not magically recompile when a hierarchy evolves.
    - **Fix:** copy mutable inputs, design null handling, and review serialized compatibility independently of source exhaustiveness.
    - **Trade-off:** records fit values, not arbitrary entity inheritance or hidden mutable lifecycle state.
    - **Follow-up:** What happens when a pattern switch receives `null` without a null case?
    - **Takeaway:** Compiler assistance complements, rather than replaces, domain and compatibility design.

### Q12. How do erasure and PECS shape a reusable generic API?

??? question "Reveal answer"
    - **Use `extends` for producers, `super` for consumers, and explicit type metadata at serialization boundaries.**
    - **Mechanism:** compilation inserts casts and bridge methods; most parameterization is erased. Generic signatures can retain metadata, but an arbitrary object's `T` is not reified.
    - **Failure:** raw types defer errors until runtime; `Class<List>` cannot describe element types. Stringifying arbitrary cache keys creates collisions and ambiguous reconstruction.
    - **Fix:** use bounded wildcards, type tokens for parameterized values, and typed key codecs instead of unchecked casts.
    - **Trade-off:** variance improves caller flexibility but nested wildcards can obscure the domain API.
    - **Follow-up:** Why cannot you safely add a `Number` to `List<? extends Number>`?
    - **Takeaway:** Preserve type information where erasure crosses an external boundary.

### Q13. How should you reason about strings, interning, and concatenation costs?

??? question "Reveal answer"
    - **Strings are immutable values; repeated growing concatenation can still create quadratic copying.**
    - **Mechanism:** literals are interned; modern OpenJDK uses compact byte-based storage where applicable. Single-expression `+` uses optimized concatenation machinery.
    - **Failure:** `==` checks references, not content; interning unbounded user input introduces global retention and contention risks.
    - **Fix:** use `equals`, and use a locally owned `StringBuilder` for repeated accumulation. Bound large inputs before building responses.
    - **Production:** profile actual allocation before micro-optimizing; character count, UTF-16 units, Unicode code points, and encoded bytes differ.
    - **Follow-up:** Why is a substring not necessarily the memory leak it was on older JVMs?
    - **Takeaway:** Separate value semantics from storage and allocation details.

### Q14. Where does `Optional` clarify an API, and where does it hide errors?

??? question "Reveal answer"
    - **Use it to represent expected absence, not operational failure.**
    - **Mechanism:** `map` composes a possible value; `flatMap` avoids nesting; `orElseGet` evaluates a fallback lazily whereas an `orElse` argument is evaluated eagerly.
    - **Failure:** returning empty after a database timeout misreports failure as missing data. Unchecked `get` merely moves null-like failure elsewhere.
    - **Fix:** return empty collections for no elements; represent failures separately; favor ordinary required parameters over Optional parameters unless absence is genuinely part of the contract.
    - **Trade-off:** Optional fields complicate serialization and frameworks; it is value-based, not a lock or identity token.
    - **Follow-up:** What should a repository return when the dependency is unavailable?
    - **Takeaway:** Missing data and failed lookup are different outcomes.

### Q15. How do you make an immutable value safe for shared caching?

??? question "Reveal answer"
    - **Eliminate observable mutation throughout the reachable value graph.**
    - **Mechanism:** final fields and non-overridable behavior prevent reassignment; defensive copies remove caller aliases. Records alone do not copy lists or arrays.
    - **Failure:** one caller mutates a cached list element and changes another request's response. A read-only view still reflects backing mutations.
    - **Fix:** copy on construction and access where necessary, use immutable elements, avoid constructor escape, and publish through a safe boundary.
    - **Trade-off:** copying costs memory and time; immutable sharing can then avoid repeated locking and copying. Measure retained size as well as entry count.
    - **Follow-up:** How would you make a record containing `byte[]` immutable?
    - **Takeaway:** Immutability is a property of aliases and reachable state.

### Q16. How should checked failures inside streams be handled?

??? question "Reveal answer"
    - **Choose fail-fast, explicit per-item results, or deliberate skipping according to the business contract.**
    - **Mechanism:** JDK function interfaces generally do not declare checked exceptions; adapters can wrap them while preserving causes. Evaluation fails at the terminal operation.
    - **Failure:** logging and dropping an order silently loses business work; sneaky throws hide the declared contract. Parallel workers may already have produced effects when failure propagates.
    - **Fix:** use `UncheckedIOException` for appropriate I/O failures, an explicit result type for imports, or a plain loop for complex recovery. Close `Files.lines` with try-with-resources.
    - **Production:** bound accumulated error reports, count rejected items, and restore interruption instead of swallowing it.
    - **Follow-up:** Does aborting a stream roll back earlier external side effects?
    - **Takeaway:** Error policy comes before pipeline syntax.

### Q17. How do lambda, method reference, anonymous class, named strategy, and enum differ?

??? question "Reveal answer"
    - **Choose the smallest representation that preserves behavior, state ownership, and testability.**
    - **Mechanism:** lambdas use lexical `this`; anonymous classes have their own instance. Method references may be static, bound, unbound, or constructor references.
    - **Failure:** capturing a mutable service or large request retains it in long-lived callbacks. Lambda identity and allocation reuse are not contractual guarantees.
    - **Fix:** use short stateless lambdas, named classes for meaningful configurable strategies, enums for closed stateless alternatives, and composition for combined predicates.
    - **Trade-off:** named types improve diagnostics and reuse but add ceremony. Avoid serializing lambdas as durable contracts.
    - **Follow-up:** When is a bound method reference's receiver evaluated?
    - **Takeaway:** Concise syntax does not remove lifetime or state concerns.

### Q18. How do list factories, copies, and views differ?

??? question "Reveal answer"
    - **Unmodifiable means callers cannot mutate through that reference; it does not imply deep immutability.**
    - **Mechanism:** `unmodifiableList` is a live view; `copyOf` is an unmodifiable snapshot rejecting nulls. `Arrays.asList` is a fixed-size array-backed view allowing `set`.
    - **Failure:** a backing list changes under readers; mutable elements change inside a snapshot. Do not infer the mutability of `Collectors.toList` from today's implementation.
    - **Fix:** specify ownership, null policy, and snapshot semantics. Use `toCollection(ArrayList::new)` for guaranteed mutability; Java 21 `Stream.toList` is unmodifiable and permits null elements.
    - **Trade-off:** snapshots cost copying; views require controlled backing-state lifetime and synchronization.
    - **Follow-up:** Can a thread-safe wrapper make unsynchronized external writes to its backing list safe?
    - **Takeaway:** Distinguish a view, a shallow copy, and a deep immutable value.

### Q19. How do you prevent collector failures and nondeterministic aggregation?

??? question "Reveal answer"
    - **Define duplicate, null, ordering, and merge semantics explicitly.**
    - **Mechanism:** `toMap` without a merge function rejects duplicate keys; collectors combine partial results during parallel reduction and require compatible associative operations.
    - **Failure:** a last-write-wins merge silently discards orders; subtraction is not an associative reduction; null values can fail collection.
    - **Fix:** reject duplicates intentionally or choose a domain-valid merge; select a map supplier for ordering. Use `groupingBy` with downstream collectors for grouped summaries.
    - **Production:** bound group cardinality and input volume; `teeing` avoids separate traversals but not the memory cost of its downstream collectors.
    - **Follow-up:** What changes when `groupingByConcurrent` is used with ordered input?
    - **Takeaway:** Collection syntax must preserve the domain's aggregation rules.

### Q20. Are `merge` and `computeIfAbsent` enough to make a multimap thread-safe?

Also relevant: [Concurrency](../concurrency/index.md)

??? question "Reveal answer"
    - **Atomic map updates do not make mutable values or multi-key invariants atomic.**
    - **Mechanism:** a concurrent map can atomically install a list reference; adding to that ordinary `ArrayList` afterward is a separate unsafe operation.
    - **Failure:** concurrent adds lose data or corrupt the list. Plain `HashMap` default methods provide no concurrency protection at all.
    - **Fix:** use immutable replacement under `compute`, a concurrent value collection, or an enclosing lock covering the whole invariant.
    - **Trade-off:** immutable replacement copies values; coarse locks serialize writers; concurrent values have their own weaker iteration guarantees.
    - **Follow-up:** How would removal of the key race with an add to its previous list?
    - **Takeaway:** Thread safety must include the objects stored inside the map.

### Q21. How do try-with-resources and suppressed exceptions preserve failure information?

??? question "Reveal answer"
    - **Resources close in reverse acquisition order; close failures are suppressed behind an earlier body failure.**
    - **Mechanism:** generated cleanup preserves the primary exception and attaches secondary exceptions through `getSuppressed`. Earlier resources also close if later initialization fails.
    - **Failure:** returning from `finally` discards errors; manually closing one resource may prevent another from closing. A successful query can still fail during cleanup.
    - **Fix:** use try-with-resources for owned resources and preserve the complete exception chain in boundary logging.
    - **Production:** transaction-aware connection ownership may belong to Spring, so use its data-access APIs rather than closing unrelated thread-bound resources manually.
    - **Follow-up:** Who owns an input stream returned by an API?
    - **Takeaway:** Resource ownership and error ownership must both be explicit.

### Q22. How do money, time, and boxed equality cause production bugs?

??? question "Reveal answer"
    - **Make units, precision, identity, and time zones explicit at boundaries.**
    - **Mechanism:** `BigDecimal.equals` includes scale; `compareTo` compares numeric value. Binary floating point cannot exactly represent many decimal fractions; boxed `==` compares references.
    - **Failure:** scale differences split hash keys; minor-unit arithmetic overflows; local timestamps become ambiguous around DST. Integer cache ranges must not drive correctness.
    - **Fix:** use explicit currency and rounding rules, checked arithmetic, value equality, `Instant` for timeline events, and injected `Clock` for tests.
    - **Trade-off:** minor units simplify fixed-scale currencies but not every asset; future local schedules need a zone and recurrence rules, not only an Instant.
    - **Follow-up:** How should a system handle a nonexistent local time during a DST transition?
    - **Takeaway:** Model domain meaning before choosing a primitive representation.

### Q23. How do you build a deterministic comparator for sorting and pagination?

??? question "Reveal answer"
    - **Specify null handling, direction, and a unique stable tie-breaker.**
    - **Mechanism:** comparator chains apply the next comparison only on ties. Reversing the entire chain differs from reversing only one component.
    - **Failure:** equal timestamps reorder across pages; subtraction-based comparisons overflow; a comparator inconsistent with equality changes sorted-set membership.
    - **Fix:** use `comparing`, primitive specializations, and explicit `nullsFirst`/`nullsLast`. Match database ordering and cursor predicates, including collation and null semantics.
    - **Trade-off:** stable sorting preserves input ties, but distributed database results need their own total ordering.
    - **Follow-up:** Why does deterministic ordering alone not guarantee snapshot-consistent pagination?
    - **Takeaway:** Stable order and stable data are separate requirements.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Correct implementations](solutions.md)
- [Concurrency practice](../concurrency/practice.md)
