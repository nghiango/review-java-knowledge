# Solution: Customer profile lookup

## Annotated code

```java
public class CustomerProfileService {
    private final ProfileRepository repository;

    // Design issue: Optional as mutable service state does not model an operation's return absence
    // and creates shared state when this service is used as a singleton.
    private Optional<CustomerProfile> lastProfile = Optional.empty();

    // API design issue: Optional parameters force every caller to wrap input and still permit null
    // for both the Optional reference and its eventual value source.
    public CustomerProfile load(Optional<String> email) {
        try {
            // Reliability issue: Optional.get() converts expected absence into a context-free
            // NoSuchElementException for either the email or repository result.
            var profile = repository.findByEmail(email.get()).get();
            lastProfile = Optional.of(profile);
            return profile;
        // Reliability issue: Catching Exception merges absence, invalid input, programming defects
        // and repository outages, then violates the non-null-looking return contract with null.
        } catch (Exception ignored) {
            return null;
        }
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Design issue | High | `lastProfile` | Optional used as mutable singleton state |
| 2 | API design issue | Medium | `load(Optional<String>)` | Optional parameter obscures input contract |
| 3 | Reliability issue | High | `email.get()` / result `get()` | Expected absence becomes generic failure |
| 4 | Reliability issue | High | `catch (Exception)` | Operational failures swallowed and null returned |

## Issue details

### Optional as mutable service state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** The last caller's profile is stored on a service likely shared by many requests.

**Why it happens:** Optional is treated as a general-purpose wrapper rather than a return-value
signal; mutable state is added to a stateless service.

**Production impact:** Callers race and can observe another customer's profile.

**Correct implementation:** `CustomerProfileService` is stateless and returns the requested value.

**Trade-offs:** Per-request state belongs in the request flow or an explicitly keyed cache.

**How to detect it:** Concurrent tests and inspection of mutable fields on singleton beans.

### Optional parameter

**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** Callers must wrap a required email without gaining a stronger contract.

**Why it happens:** Optional is used outside its primary role as a return type.

**Production impact:** Double absence (`null` Optional vs empty Optional) and noisy call sites.

**Correct implementation:** Accept `String`, validate at the boundary, normalize once.

**Trade-offs:** Optional parameters can still be reasonable in internal fluent APIs with a clear
convention, but are poor default public contracts.

**How to detect it:** API review and nullability tests.

### Unchecked Optional access

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

**Problem:** `get()` throws without domain context when either value is absent.

**Why it happens:** The happy path is extracted before absence is mapped.

**Production impact:** Generic 500 responses and logs without the requested identity.

**Correct implementation:** `orElseThrow(() -> new CustomerNotFoundException(email))`.

**Trade-offs:** A domain exception creates an explicit mapping obligation at the delivery boundary.

**How to detect it:** Absence tests and searches for zero-argument `Optional.get()`.

### Broad catch and null return

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Repository outages and programming defects are silently converted to null.

**Why it happens:** The catch block is used as control flow for unrelated failure modes.

**Production impact:** Misleading null failures occur later; alerts never see the original outage.

**Correct implementation:** Validate input, map only expected absence, propagate infrastructure
failure unchanged.

**Trade-offs:** Callers must intentionally map each failure category to their transport contract.

**How to detect it:** Failure-injection tests and logs showing secondary NullPointerExceptions.

## Correct implementation

Package: `lab.corejava.optionalerrors`

- `src/main/java/lab/corejava/optionalerrors/CustomerProfileService.java`
- `docs/topics/core-java/solutions.md#explicit-absence-and-failure`
