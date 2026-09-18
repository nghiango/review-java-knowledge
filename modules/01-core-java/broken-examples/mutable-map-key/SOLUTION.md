# Solution: Mutable customer risk-cache key

## Annotated code

```java
public class CustomerKey {
    private final String tenantId;
    private String customerId;
    private String displayName;
    private final List<String> tags;

    // Maintainability issue: The caller-owned list is stored and returned directly, so callers
    // can mutate state that belongs to the key without going through this type's API.
    public List<String> getTags() {
        return tags;
    }

    // Design issue: Value equality is implemented on a non-final class with instanceof. A subtype
    // can add state and produce asymmetric or non-transitive equality.
    @Override
    public boolean equals(Object candidate) {
        if (!(candidate instanceof CustomerKey other)) return false;
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(customerId, other.customerId);
    }

    // Design issue: hashCode includes displayName while equals does not. Equal keys can therefore
    // have different hashes and occupy different buckets.
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, customerId, displayName);
    }
}

public class CustomerRiskCache {
    private final Map<CustomerKey, String> riskByCustomer = new HashMap<>();

    // Data consistency issue: Mutating customerId after insertion changes the key's hash. HashMap
    // still stores the entry in the old bucket, making it unreachable through normal lookup.
    public void refreshCustomerId(CustomerKey key, String canonicalCustomerId) {
        key.setCustomerId(canonicalCustomerId);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Maintainability issue | Medium | `CustomerKey.getTags()` | Mutable internal collection escapes |
| 2 | Design issue | High | `CustomerKey.equals()` | Extensible value type makes equality unsafe |
| 3 | Design issue | High | `CustomerKey.hashCode()` | Equality and hash use different state |
| 4 | Data consistency issue | High | `CustomerRiskCache.refreshCustomerId()` | Map key changes after insertion |

## Issue details

### Mutable collection escapes

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** The key stores and exposes the caller's mutable list.

**Why it happens:** `List` is an interface, not an immutability guarantee; both constructor input
and accessor output alias the same object.

**Production impact:** Unrelated code can change key-associated state, producing order-dependent
behaviour and unsafe publication between threads.

**Correct implementation:** `CustomerSnapshot` applies `List.copyOf`; identity is separated into
`CustomerKey`.

**Why the solution works:** Neither the constructor input nor accessor can mutate the captured list.

**Trade-offs:** Copying costs O(n) and rejects null elements, which becomes part of the contract.

**How to detect it:** Mutation-focused tests and static analysis for exposed mutable collections.

**Interview follow-up:** When is an unmodifiable view insufficient compared with a defensive copy?

### Extensible value equality

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** A subtype can add equality-significant state while the base class accepts it via
`instanceof`, breaking symmetry or transitivity.

**Why it happens:** Equality and open inheritance impose competing substitutability contracts.

**Production impact:** Sets, maps and deduplication produce inconsistent results across subclasses.

**Correct implementation:** The record is final and has compiler-generated value equality.

**Why the solution works:** No subtype can alter the equality domain.

**Trade-offs:** Composition must replace inheritance when additional behaviour is needed.

**How to detect it:** Equality-contract tests across base and subtype instances.

**Interview follow-up:** Compare `getClass()` and `instanceof` in entity and value-object equality.

### Equality and hash use different state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Two equal keys with different display names can have different hash codes.

**Why it happens:** `equals` uses tenant/customer identity while `hashCode` also uses display name.

**Production impact:** HashMap lookup and HashSet duplicate detection become unreliable.

**Correct implementation:** `CustomerKey` contains only identity components; record-generated
`equals` and `hashCode` use the same state.

**Why the solution works:** Equal records necessarily produce equal hashes.

**Trade-offs:** Display data must be stored separately from identity.

**How to detect it:** Property tests asserting `a.equals(b) -> a.hashCode() == b.hashCode()`.

**Interview follow-up:** Must unequal objects have different hash codes?

### Key mutation after insertion

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Changing `customerId` changes the bucket calculation after the entry is stored.

**Why it happens:** HashMap does not re-index entries when a key mutates.

**Production impact:** Cache misses appear despite the entry consuming memory; removal may also fail.

**Correct implementation:** Use immutable `lab.corejava.mutablemapkey.CustomerKey` and create a new
key when identity changes.

**Why the solution works:** Hash/equality state is stable for the key's lifetime.

**Trade-offs:** Identity changes require explicit remove/reinsert or migration logic.

**How to detect it:** Reproduce lookup before/after mutation; inspect map entries and hash values.

**Interview follow-up:** Why does `ConcurrentHashMap` not solve mutable-key corruption?

## Correct implementation

Package: `lab.corejava.mutablemapkey`

- `src/main/java/lab/corejava/mutablemapkey/CustomerKey.java`
- `src/main/java/lab/corejava/mutablemapkey/CustomerSnapshot.java`
- `docs/topics/core-java/solutions.md#immutable-map-identity`
