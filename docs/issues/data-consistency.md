# Data Consistency Issues

Lost updates, stale state, partial writes and identity/invariant failures.

## Entries

### Mutable key after HashMap insertion

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java, HashMap · **Interview frequency:** High · **Production impact:** High

A HashMap records the insertion-time bucket; it cannot observe later mutation of equality/hash
state. The entry consumes memory but normal lookup and removal can miss it. Use an immutable
identity key and explicitly remove/reinsert when identity genuinely changes.

**Detection:** Compare lookup and hash values before/after mutation; inspect entries in a debugger.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

**Trade-off:** Immutable identity separates profile data and requires explicit identity migration.

**Interview follow-up:** Why does ConcurrentHashMap not repair a mutated key?

## Related

- [Issue catalogue](index.md)
