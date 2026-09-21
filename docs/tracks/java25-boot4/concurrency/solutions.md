# Solutions: Concurrency Refactoring in Java 25

!!! info "Delta from baseline"
    Baseline solutions in [`docs/topics/concurrency/solutions.md`](../../../topics/concurrency/solutions.md) resolve classical locks, atomic variables, and bounded thread pools.
    This page details production solutions for **Java 25 Concurrency traps**: Scoped Values for zero-cleanup context propagation, and Structured Task Scopes for coordinated cancellation.

---

## Solution 1: Resolving Virtual Thread Context Leaks with `ScopedValue`

--8<-- "tracks/java25-boot4/modules/03-concurrency/broken-examples/threadlocal-leak-virtual-threads/SOLUTION.md"

### Production Reference Implementation

Here is the clean production implementation using `ScopedValue`:

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/main/java/lab/java25boot4/concurrency/ScopedValueSecurityContext.java"
```

### Key Architectural Benefits
- **Zero Overhead Thread Spawning**: Virtual threads no longer clone a hash map.
- **Dynamic Scope Invariant**: Context is impossible to leak past the execution block boundary.

---

## Solution 2: Eliminating Orphan Tasks with `StructuredTaskScope`

--8<-- "tracks/java25-boot4/modules/03-concurrency/broken-examples/unstructured-concurrency-orphan-tasks/SOLUTION.md"

### Production Reference Implementation

Here is the production implementation refactored to `StructuredTaskScope`:

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/main/java/lab/java25boot4/concurrency/StructuredConcurrencyOrderAggregator.java"
```

### Fastest-First Winner Query Pattern

When racing multiple external replicas and needing only the fastest successful response:

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/main/java/lab/java25boot4/concurrency/RaceWinnerStructuredQuery.java"
```
