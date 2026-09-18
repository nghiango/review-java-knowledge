# Core Java

## Why this matters

Framework expertise cannot compensate for unstable equality, leaked resources or unsafe shared
state. Senior engineers must connect Java contracts to production symptoms and API trade-offs.

## Core Concepts

- [Object design, equality, collections, generics, exceptions and streams](concepts.md)
- [HashMap, type erasure, String and stream internals](internals.md)

## How it works internally

Follow a HashMap lookup from spread hash to bucket and equality checks; then examine type erasure,
compact strings and lazy stream evaluation in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) progresses from contracts to production diagnosis.

## Common Production Problems

Mutable map keys, partial imports, leaked descriptors, swallowed failures and common-pool starvation
are covered in [Production](production.md).

## Broken Examples

1. [Mutable map key](code-review.md#mutable-map-key)
2. [Optional and exception misuse](code-review.md#optional-and-exception-misuse)
3. [Resource and collection mutation](code-review.md#resource-and-collection-mutation)
4. [Stream and parallel side effects](code-review.md#stream-and-parallel-side-effects)

## Correct Implementations

Each exercise maps to tested source in [Solutions](solutions.md).

## Trade-offs

Immutability introduces copies; explicit error types enlarge API surface; deterministic concurrency
requires executor ownership. The module makes these costs visible rather than prescribing slogans.

## Production Checklist

- Equality and hash state are stable and compatible.
- Public boundaries distinguish absence, invalid input and operational failure.
- The code that opens a resource owns or explicitly transfers closure.
- Stream pipelines avoid external mutation.
- Blocking concurrency uses a bounded, monitored executor.

## Senior-Level Questions

Can you explain why a cache misses an entry that still appears during iteration, or why low CPU can
coexist with common-pool starvation? Use the [senior questions](questions.md#senior).

## Exercises

Apply the contracts without copying the examples in [Exercises](exercises.md).

## Further Experiments

Change key state after insertion, inspect suppressed exceptions, compare ordered and unordered
parallel operations, and profile blocking work in different executors.

## Related

- [JVM roadmap](../../roadmap.md)
- [Concurrency issues](../../issues/concurrency.md)
- [Maintainability issues](../../issues/maintainability.md)
