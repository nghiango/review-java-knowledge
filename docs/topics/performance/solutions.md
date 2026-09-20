# Performance Solutions

## Connection pool budget and short resource scopes

```java
--8<-- "modules/23-performance/src/main/java/lab/performance/connectionpool/ConnectionPoolBudget.java"
```

The budget reserves operational connections and divides remaining capacity across instances.
`PoolConfiguration` adds a finite acquisition timeout. The checkout workflow still needs short
transactions, idempotent payment, and reconciliation; pool sizing cannot fix a long boundary.

## Bounded workload-aware executors

```java
--8<-- "modules/23-performance/src/main/java/lab/performance/threadpool/BoundedExecutorFactory.java"
```

Worker count follows an explicit workload profile. The bounded queue and rejection policy preserve
a latency and memory envelope. Rejection moves overload handling to the caller, where deadlines,
load shedding, or a retry budget can be applied.

## Bounded batch loading

```java
--8<-- "modules/23-performance/src/main/java/lab/performance/batchloading/OrderSummaryLoader.java"
```

The loader validates the result limit, deduplicates customer IDs, performs one batch lookup, and
fails explicitly for missing data. A join may be faster for some schemas; compare query plans, row
multiplication, and payload size.

## Allocation-conscious encoding

```java
--8<-- "modules/23-performance/src/main/java/lab/performance/allocation/MetricLineEncoder.java"
```

One operation-local builder avoids shared mutable state and most intermediate representations.
Sorted tags make the wire value deterministic. The remaining stream sort is a clarity trade-off;
change it only after an allocation profile shows material cost.

## Contention-friendly accumulation

```java
--8<-- "modules/23-performance/src/main/java/lab/performance/contention/ConcurrentMetricAccumulator.java"
```

Independent keys update independently, and `LongAdder` stripes writes under contention. Reads are
appropriate for telemetry snapshots; financial or transactional totals may require stronger
consistency.

## Related

- [Code review](code-review.md)
- [Tests](tests.md)
- [Production](production.md)
