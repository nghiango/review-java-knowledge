# Core Java Exercises

## 1. Case-insensitive email identity

Design an immutable email key whose equality is case-insensitive. Reject blank values and ensure
HashMap lookup works with differently cased input. Decide whether to preserve original spelling.

??? success "Solution"
    Normalize once in the constructor with `strip().toLowerCase(Locale.ROOT)` and store the
    normalized value in a record. Equality/hash then use identical stable state. Preserve display
    spelling in a separate profile value if required; do not make equality call locale-sensitive
    lowercasing repeatedly.

## 2. Remove a side-effecting parallel collector

A pipeline calls a blocking client and appends to a shared list. Preserve input order, cap calls at
four and retain item identity in failures.

??? success "Solution"
    Accept a caller-owned fixed executor of size four. Create one `CompletableFuture` per immutable
    input, wrap failures with the input identifier, and join the future list in encounter order.
    The caller owns shutdown and monitoring. Prefer sequential execution unless measurement shows
    the concurrent complexity is justified.

## Further experiments

- Make the email key locale-sensitive and explain the Turkish-I failure.
- Let completion order differ from input order, then compare ordered join with unordered emission.
- Throw from both CSV reading and close; inspect `getSuppressed()`.

## Related

- [Concepts](concepts.md)
- [Solutions](solutions.md)
- [Questions](questions.md)
