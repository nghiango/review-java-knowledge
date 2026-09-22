# Code Review: Unsafe Off-Heap Buffer

## Scenario

`UnsafeOffHeapBuffer.java` is added as part of a Java 25 migration. The metric-ingestion hot path
allocates a fixed number of `long` samples off-heap so that they do not add GC pressure.

The pull request description says:

> *"We moved the sample buffer off-heap with `Unsafe.allocateMemory`. It is the fastest option and
> it removes the buffer from the garbage collector's reach entirely."*

## Review Objectives

1. Is `sun.misc.Unsafe` memory access a supported long-term choice on Java 25?
2. What happens to the allocated native memory if an operation fails or the caller forgets to close?
3. Can a caller write outside the allocated region?
4. Is this the design you would approve for a production service, or is there a supported API that
   achieves the same goal?

Consider these dimensions:

- deprecation and platform support
- resource lifecycle and leaks
- memory safety and bounds
- maintainability
- failure handling

Write your findings down before opening `SOLUTION.md`.
