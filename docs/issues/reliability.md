# Reliability Issues

Failure handling, configuration, deployment and testing weaknesses that reduce dependable service.

## Entries

### Unchecked Optional access

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Calling `Optional.get()` without proving presence converts expected absence into a context-free
exception. Map absence explicitly with `orElseThrow` or transform it with `map`/`flatMap`.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Broad exception catch hides an outage

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Catching `Exception` around repository access merges absence, invalid input, defects and outages.
Handle only the failure the layer owns; preserve operational failures and their causes.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Structural mutation invalidates iteration

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Removing through a collection while an enhanced-for Iterator is active changes its modification
count and fails fast. Use iterator removal, `removeIf`, or derive a new immutable result.

### Abrupt JVM Termination Drops In-Flight Tasks

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Default `server.shutdown: immediate` and unmanaged `ExecutorService` instances forcefully terminate
running worker threads and sever active HTTP connections upon receiving `SIGTERM`. Configure
`server.shutdown: graceful` and manage worker threads via `ThreadPoolTaskExecutor` with
`setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)`.

**Appears in:** `modules/05-spring-boot/broken-examples/no-graceful-shutdown`

## Related

- [Issue catalogue](index.md)
