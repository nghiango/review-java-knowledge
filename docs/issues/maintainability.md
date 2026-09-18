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

### Optional as mutable shared state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

An Optional field on a singleton service is mutable cross-request state, not an absence contract.
Keep the service stateless or use an explicitly keyed, thread-safe cache.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Optional parameter obscures a required contract

**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic

An Optional parameter forces wrapping while still allowing a null Optional reference. Accept the
required value and validate it at the boundary; reserve Optional primarily for return absence.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

## Related

- [Issue catalogue](index.md)
