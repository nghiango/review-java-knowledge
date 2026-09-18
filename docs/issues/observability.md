# Observability Issues

Missing context, unsafe or absent logs, weak metrics and broken trace propagation.

## Entries

### Asynchronous failure loses operation context

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

Replacing an exception without its cause and input identity makes concurrent failures impossible to
attribute. Preserve the cause and a safe domain identifier in a typed exception.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

## Related

- [Issue catalogue](index.md)
