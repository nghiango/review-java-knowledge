# Maintainability and Design Issues

Coupling, weak abstractions, API contracts, encapsulation and architecture failures.

## Entries

### Exposed mutable collection

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** Core Java collections · **Interview frequency:** Medium · **Production impact:** Medium

Storing and returning a caller-owned list aliases mutable state across boundaries. Use `List.copyOf`
when taking a snapshot; an unmodifiable view alone still reflects mutation through the original list.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Extensible value-object equality

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java equality · **Interview frequency:** High · **Production impact:** High

Open inheritance and value equality can violate symmetry/transitivity when a subtype adds state.
Prefer a final immutable value type (a record where appropriate) or explicitly define identity
semantics for the hierarchy.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Inconsistent equals and hashCode state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** HashMap, HashSet · **Interview frequency:** High · **Production impact:** High

`equals` and `hashCode` must use compatible state: equal values must always have equal hashes.
Keep display/profile data outside the identity key and test the implication directly.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

## Related

- [Issue catalogue](index.md)
