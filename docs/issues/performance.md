# Performance Issues

Latency, allocation, resource leaks, saturation and scalability failures.

## Entries

### Reader leak exhausts file descriptors

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Basic

A method that creates a reader owns its closure. Without try-with-resources, repeated imports retain
OS descriptors on success and failure until the process can no longer open files.

**Detection:** Process open-file metrics, `lsof -p <pid>` and failure-path tests with a close-tracking reader.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Blocking I/O in the common ForkJoinPool

**Type:** Performance issue · **Severity:** High · **Difficulty:** Senior

Parallel streams share a process-wide pool tuned for CPU work. Blocking price calls can starve
unrelated streams and CompletableFuture stages. Prefer sequential work or an explicit bounded,
monitored executor.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Static listener retention

**Type:** Memory issue · **Severity:** High · **Difficulty:** Intermediate

A static listener list keeps strong references to listeners and everything they capture for as long
as the defining class loader remains reachable. Prefer an instance-owned registry whose lifetime is
bound to the component that owns it.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Missing registration lifecycle

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

Registering a listener without returning a closeable registration handle makes cleanup depend on
out-of-band knowledge. Return an explicit handle and close it during component shutdown.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

## Related

- [Issue catalogue](index.md)
