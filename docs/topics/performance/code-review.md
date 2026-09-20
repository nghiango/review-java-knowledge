# Performance Code Review

Investigate each target before opening its reveal. Run the finite load recipe from its `REVIEW.md`
and record latency distributions, errors, and resource saturation.

## Hikari exhaustion

```java
--8<-- "modules/23-performance/broken-examples/hikari-exhaustion/CheckoutService.java"
```

??? warning "Reveal issues"

    - **Resource leak issue:** the connection remains checked out during payment I/O.
    - **Transaction issue:** remote latency extends locks and transaction lifetime.
    - **Reliability issue:** a successful charge and failed commit can diverge.

See [the corrected design](solutions.md#connection-pool-budget-and-short-resource-scopes).

## Oversized thread pool

```java
--8<-- "modules/23-performance/broken-examples/oversized-thread-pool/ReportExecutor.java"
```

??? warning "Reveal issues"

    - **Performance issue:** 500 CPU workers oversubscribe processors.
    - **Scalability issue:** the implicit queue is unbounded.
    - **Reliability issue:** callers have no overload contract.

See [the bounded executor](solutions.md#bounded-workload-aware-executors).

## N+1 summaries

```java
--8<-- "modules/23-performance/broken-examples/n-plus-one-summary/OrderSummaryService.java"
```

??? warning "Reveal issues"

    - **API design issue:** the work limit is not bounded.
    - **Database issue:** one customer query runs for each order.
    - **Performance issue:** duplicate customer IDs repeat I/O.

See [batch loading](solutions.md#bounded-batch-loading).

## Excessive allocation

```java
--8<-- "modules/23-performance/broken-examples/excessive-allocation/MetricLineEncoder.java"
```

??? warning "Reveal issues"

    - **Performance issue:** regex, maps, streams, formatting, and strings allocate per event.
    - **Observability issue:** every telemetry value is logged.

See [the allocation-conscious encoder](solutions.md#allocation-conscious-encoding).

## Lock contention

```java
--8<-- "modules/23-performance/broken-examples/lock-contention/MetricAccumulator.java"
```

??? warning "Reveal issues"

    - **Concurrency issue:** independent keys share one monitor.
    - **Performance issue:** every update boxes under the lock.
    - **Scalability issue:** reads serialize with writers.

See [the concurrent accumulator](solutions.md#contention-friendly-accumulation).

## Related

- [Solutions](solutions.md)
- [Performance issues](../../issues/performance.md)
- [Production diagnostics](production.md)
