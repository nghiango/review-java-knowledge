# Core Java Code Review

Review each clean source before expanding its answer.

## Mutable map key

A risk cache loses entries during profile refresh.

```java
--8<-- "modules/01-core-java/broken-examples/mutable-map-key/CustomerKey.java"
```

```java
--8<-- "modules/01-core-java/broken-examples/mutable-map-key/CustomerRiskCache.java"
```

Consider equality, hashing, mutation, encapsulation and inheritance.

??? warning "Reveal issues"
    **Data consistency issue — mutated key:** HashMap stores the insertion bucket and never
    re-indexes a key. Changing identity makes lookup/removal miss an entry that still consumes
    memory. Compare hashes before/after mutation; fix with immutable identity. Identity migration
    must then be explicit.

    **Design issue — incompatible equality/hash state:** `hashCode` includes display name while
    `equals` does not, so equal values can enter different buckets. Test the implication
    `a.equals(b) → equal hashes`; keep profile data outside identity.

    **Design issue — extensible value equality:** A subtype can add state and break symmetry or
    transitivity. A final record closes the equality domain, trading inheritance for composition.

    **Maintainability issue — escaped list:** Constructor and accessor alias mutable tags. Mutation
    tests expose it; `List.copyOf` creates a snapshot at O(n) cost and rejects null elements.

[Correct design](solutions.md#immutable-map-identity)

## Optional and exception misuse

A profile endpoint must distinguish invalid input, absence and repository outage.

```java
--8<-- "modules/01-core-java/broken-examples/optional-exception-misuse/CustomerProfileService.java"
```

Consider Optional boundaries, shared state, nullability and exception ownership.

??? warning "Reveal issues"
    **Design issue — shared state:** `lastProfile` belongs to whichever caller ran last and races in
    a singleton. Concurrency inspection detects it; a stateless service removes cross-request state.

    **API design issue — Optional parameter:** Callers must wrap a required value while a null
    Optional remains possible. Accept and validate the value directly; reserve Optional for return
    absence unless an internal API has a documented reason.

    **Reliability issue — unchecked `get()`:** Expected absence becomes context-free
    `NoSuchElementException`. Absence tests expose it; contextual `orElseThrow` makes mapping explicit.

    **Reliability issue — broad catch:** Repository outages and defects become null, causing later,
    misleading failures. Inject repository failure and assert its cause survives; callers must then
    deliberately map each failure category.

[Correct design](solutions.md#explicit-absence-and-failure)

## Resource and collection mutation

A long-lived importer eventually fails and may leave partial caller state.

```java
--8<-- "modules/01-core-java/broken-examples/resource-collection-mutation/CustomerCsvImporter.java"
```

Consider resource ownership, iteration, partial failure and responsibility boundaries.

??? warning "Reveal issues"
    **Resource leak issue:** The importer creates but never closes the reader. Open-descriptor
    metrics and close-tracking tests expose it; try-with-resources closes success and failure paths.

    **Data consistency issue:** Valid early rows mutate caller state before a later parse failure.
    Failure injection reveals the partial import; return an immutable classified result. Buffering
    costs memory, so large imports may need transactional chunks.

    **Reliability issue:** Enhanced-for iteration owns an Iterator, but removal occurs through the
    list and invalidates its modification count. Use iterator removal, `removeIf`, or derive a new list.

    **Design issue:** I/O, syntax, validation and mutation cannot vary independently. Injecting a
    source and returning values increases API surface but makes ownership and failure policy testable.

[Correct design](solutions.md#resource-safe-csv-processing)

## Stream and parallel side effects

A request path prices orders through a blocking remote client.

```java
--8<-- "modules/01-core-java/broken-examples/stream-parallel-side-effects/OrderReportService.java"
```

Consider shared mutation, common-pool ownership, order and failure context.

??? warning "Reveal issues"
    **Concurrency issue:** Common-pool workers mutate one ArrayList without synchronization, losing
    or corrupting rows. Cardinality tests under contention expose it; pure mapping removes shared state.

    **Performance issue:** Blocking lookups park shared ForkJoin workers and starve unrelated work.
    Thread dumps and pool metrics expose it; use sequential work or a bounded, monitored executor.

    **Observability issue:** A replacement exception drops cause and order identity. Failure
    injection exposes the loss; a typed exception retains safe context.

    **Data consistency issue:** Parallel `forEach` does not order side effects, and races threaten
    completeness. Join immutable futures in input order; this can wait behind an earlier slow item.

[Correct design](solutions.md#deterministic-order-reporting)

## Related

- [Solutions](solutions.md)
- [Issue catalogue](../../issues/index.md)
- [Questions](questions.md)
