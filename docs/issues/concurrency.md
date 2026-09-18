# Concurrency Issues

Atomicity, visibility, ordering, shared mutation, deadlock and starvation failures.

## Entries

### Shared mutable accumulator in a parallel stream

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel workers cannot safely append to one ArrayList. Use a pure mapping/collector whose reduction
owns its state, or submit independent values to an explicit executor and join immutable results.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

## Related

- [Issue catalogue](index.md)
