# Solution: Static listener leak

## Annotated code

```java
package lab.jvm.broken.staticlistenerleak;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GlobalEventRegistry {
    // Memory issue: A static collection retains every listener for the lifetime of the class
    // loader, including listeners owned by components that have already stopped.
    // Resource leak issue: register() returns no lifecycle handle, so callers have no reliable
    // way to unregister listeners when their owning resource closes.
    private static final List<Consumer<String>> LISTENERS = new ArrayList<>();

    private GlobalEventRegistry() {}

    public static void register(Consumer<String> listener) {
        // Design issue: Duplicate registrations are accepted silently, so repeated startup hooks
        // or retries can deliver the same event multiple times and retain the same owner twice.
        LISTENERS.add(listener);
    }

    public static List<Consumer<String>> listeners() {
        // Maintainability issue: The backing list escapes, allowing callers to mutate global
        // registry state without using the registry API.
        return LISTENERS;
    }

    public static void publish(String event) {
        for (Consumer<String> listener : LISTENERS) {
            listener.accept(event);
        }
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Memory issue | High | `LISTENERS` | Static collection retains listeners for the class-loader lifetime |
| 2 | Resource leak issue | High | `register()` | Callers receive no lifecycle handle for unregistering |
| 3 | Design issue | Medium | `register()` | Duplicate registrations are accepted silently |
| 4 | Maintainability issue | Medium | `listeners()` | Backing list escapes to callers |

## Issue details

### Static listener retention

**Type:** Memory issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** The static list keeps strong references to every listener until the defining class
loader can be collected.

**Why it happens:** Static fields are reachable from the class metadata. A listener often captures
service instances, caches or class-loader-owned resources.

**Production impact:** Restarted components, redeployments or repeated initialization paths retain
old object graphs, causing heap growth and stale event delivery.

**Correct implementation:** `lab.jvm.listenerregistry.ListenerRegistry` owns listeners per registry
instance and can be discarded with its owning component.

**Why the solution works:** Listener lifetime is tied to an object, not process-global static state.

**Trade-offs:** Callers must pass a registry instance through their composition root instead of
calling a global helper.

**How to detect it:** Heap dump dominator trees, retained-size growth after component shutdown and
listener-count metrics.

**Interview follow-up:** Why can a static field prevent an application class loader from being
collected?

### Missing registration lifecycle

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Registering a listener has no corresponding close or unregister operation.

**Why it happens:** The API models subscription as a one-way side effect.

**Production impact:** Temporary listeners survive beyond their useful lifetime and continue doing
work after their owner is closed.

**Correct implementation:** `ListenerRegistry.register()` returns a `Registration` handle whose
`close()` method removes exactly that registration.

**Why the solution works:** The owner can use try-with-resources or close the registration during
shutdown.

**Trade-offs:** Callers must keep the handle and close it, so ownership becomes explicit.

**How to detect it:** Tests that register, close and assert listener count; heap dump paths from
the registry to stopped components.

**Interview follow-up:** Why is `AutoCloseable` useful for listener registration handles?

### Duplicate listener registration

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** The same listener can be registered repeatedly with no visibility or handle.

**Why it happens:** The API has no registration identity and no duplicate policy.

**Production impact:** A retried startup path can deliver every event multiple times and retain the
same object graph multiple times.

**Correct implementation:** A `Registration` represents one subscription and closing it removes
that subscription exactly once.

**Why the solution works:** Subscription identity is explicit even when listener instances compare
the same way.

**Trade-offs:** The registry still needs a conscious product decision about whether duplicate
listeners should be allowed, rejected or de-duplicated.

**How to detect it:** Startup retry tests and listener-count metrics by owner/component.

**Interview follow-up:** Why is duplicate subscription a design problem even when memory is
bounded?

### Backing list escapes

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** `listeners()` returns the mutable registry list itself.

**Why it happens:** The accessor exposes representation instead of a snapshot or read-only view.

**Production impact:** Callers can clear, reorder or add listeners outside the registry contract.

**Correct implementation:** `ListenerRegistry.snapshot()` returns `List.copyOf(...)`.

**Why the solution works:** Callers can inspect the state they asked for without mutating registry
internals.

**Trade-offs:** Snapshot allocation costs grow with listener count.

**How to detect it:** Mutation tests that attempt to alter the returned list.

**Interview follow-up:** How does an immutable snapshot differ from an unmodifiable view?

## Correct implementation

Package: `lab.jvm.listenerregistry`

- `src/main/java/lab/jvm/listenerregistry/ListenerRegistry.java`
- `src/main/java/lab/jvm/listenerregistry/Registration.java`
- `docs/topics/jvm/solutions.md#lifecycle-aware-listener-registration`
