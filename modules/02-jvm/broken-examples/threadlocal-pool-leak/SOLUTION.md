# Solution: ThreadLocal pool leak

## Annotated code

```java
package lab.jvm.broken.threadlocalpoolleak;

import java.util.Optional;

public final class RequestContext {
    // Resource leak issue: Static ThreadLocal values remain attached to reused worker threads
    // until explicitly removed, retaining request/user data beyond the request lifetime.
    // Design issue: The API exposes setUser() without a closeable scope, so callers cannot model
    // ownership or reliably restore the previous value for nested work.
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private RequestContext() {}

    public static void setUser(String userId) {
        CURRENT_USER.set(userId);
    }

    public static Optional<String> currentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }
}
```

```java
package lab.jvm.broken.threadlocalpoolleak;

public final class RequestHandler {
    public String handle(String requestId, String userId) {
        RequestContext.setUser(userId);
        // Data consistency issue: There is no finally block that clears or restores the context,
        // so failure halfway through request processing leaves stale state on the worker thread.
        return "handled " + requestId + " for " + RequestContext.currentUser().orElse("anonymous");
    }

    public String handleAnonymous(String requestId) {
        // Security issue: Anonymous work can observe the authenticated user left behind by an
        // earlier task on the same pooled thread.
        return "handled " + requestId + " for " + RequestContext.currentUser().orElse("anonymous");
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resource leak issue | High | `RequestContext.CURRENT_USER` | Static ThreadLocal values stay attached to pooled threads |
| 2 | Design issue | High | `RequestContext.setUser()` | API has no closeable scope or parent restoration |
| 3 | Data consistency issue | High | `RequestHandler.handle()` | Request context is not cleared in a finally block |
| 4 | Security issue | High | `RequestHandler.handleAnonymous()` | Anonymous work can inherit a previous authenticated user |

## Issue details

### Static ThreadLocal value retention

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Values stored in a static `ThreadLocal` remain associated with long-lived worker
threads until removed.

**Why it happens:** Executor threads are reused across requests. Setting a value does not give it a
request lifetime.

**Production impact:** User identifiers and request objects can be retained after the request ends,
increasing heap usage and confusing later work on the same thread.

**Correct implementation:** `lab.jvm.requestcontext.RequestContext` returns a `ContextScope` whose
`close()` removes the value when there was no parent scope.

**Why the solution works:** The request lifetime is lexical and explicit; try-with-resources runs
cleanup on success and failure.

**Trade-offs:** Callers must open the scope at the boundary and keep request data small.

**How to detect it:** Single-thread executor tests, heap dumps showing retained request objects and
logs where later requests inherit old IDs.

**Interview follow-up:** Why do ThreadLocal leaks show up more often with pooled threads than with
short-lived threads?

### Missing scoped ownership

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** `setUser()` mutates ambient state without returning an object that owns restoration.

**Why it happens:** The API models context as a mutable global slot.

**Production impact:** Nested request work, impersonation and tests can leave the wrong identity in
place after the inner operation ends.

**Correct implementation:** `RequestContext.open(identity)` captures the previous identity and the
returned `ContextScope.close()` restores it.

**Why the solution works:** Parent restoration is part of the same operation that changes context.

**Trade-offs:** The API is slightly more verbose, but the lifetime becomes visible in code review.

**How to detect it:** Tests that open nested scopes and close them out of the happy path.

**Interview follow-up:** How does lexical scoping reduce ambient-context bugs?

### Missing finally cleanup

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** If request processing throws after `setUser()`, the stale user remains on the thread.

**Why it happens:** Cleanup is not tied to `try`/`finally` or try-with-resources.

**Production impact:** Metrics, logs and downstream authorization decisions can be attributed to the
wrong request or user.

**Correct implementation:** Use `try (ContextScope ignored = context.open(identity)) { ... }`.

**Why the solution works:** Java closes the scope even when the block exits with an exception.

**Trade-offs:** The boundary must be clear; asynchronous handoff still needs explicit propagation.

**How to detect it:** Tests that throw inside a scoped block and then assert `current()` is empty.

**Interview follow-up:** Why is a successful-path cleanup test insufficient?

### Stale authenticated user leak

**Type:** Security issue · **Severity:** High · **Difficulty:** Senior

**Problem:** Anonymous work can read an authenticated user left behind by an earlier task.

**Why it happens:** Thread reuse makes ambient state visible across logical requests unless it is
cleared.

**Production impact:** Audit logs, personalization and authorization checks may run under the wrong
principal.

**Correct implementation:** `RequestContext.current()` returns empty after the previous scope closes
on the same worker thread.

**Why the solution works:** Closing the scope removes the value from the thread-local map.

**Trade-offs:** Request context does not automatically propagate to other threads; propagation must
be explicit and similarly scoped.

**How to detect it:** Submit authenticated and anonymous tasks sequentially to a single-thread
executor and assert the anonymous task sees no identity.

**Interview follow-up:** Why can ThreadLocal-based security context propagation be dangerous in
async code?

## Correct implementation

Package: `lab.jvm.requestcontext`

- `src/main/java/lab/jvm/requestcontext/RequestIdentity.java`
- `src/main/java/lab/jvm/requestcontext/RequestContext.java`
- `src/main/java/lab/jvm/requestcontext/ContextScope.java`
- `docs/topics/jvm/solutions.md#scoped-request-context`
