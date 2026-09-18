# Performance Issues

Latency, allocation, resource leaks, saturation and scalability failures.

## Entries

### Reader leak exhausts file descriptors

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Basic

A method that creates a reader owns its closure. Without try-with-resources, repeated imports retain
OS descriptors on success and failure until the process can no longer open files.

**Detection:** Process open-file metrics, `lsof -p <pid>` and failure-path tests with a close-tracking reader.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

## Related

- [Issue catalogue](index.md)
