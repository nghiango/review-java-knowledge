# Performance Tests

Unit tests prove correctness and resource-policy invariants. They deliberately do not assert elapsed
time, allocation counts, or throughput because shared CI hosts make such tests unstable.

| Test | Invariant |
|---|---|
| `ConnectionPoolBudgetTest` | Global DB budget becomes a finite per-instance cap |
| `BoundedExecutorFactoryTest` | Saturated workers and queue reject additional work |
| `OrderSummaryLoaderTest` | Multiple rows perform one deduplicated customer lookup |
| `MetricLineEncoderTest` | Encoding is deterministic, escaped, and defensively copied |
| `ConcurrentMetricAccumulatorTest` | Concurrent increments are retained |

```bash
./gradlew :modules:23-performance:test
./gradlew :modules:23-performance:compileBrokenExamples
./gradlew :modules:23-performance:compileExamples
```

Performance claims belong in JMH, JFR, or load-test reports with workload, environment, warmup,
forks, and raw distributions recorded.

## Related

- [Solutions](solutions.md)
- [Production](production.md)
- [Exercises](exercises.md)
