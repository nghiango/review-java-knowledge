# Solution: Parallel order report

## Annotated code

```java
public List<PricedOrder> generate(List<Order> orders) {
    // Concurrency issue: Multiple common-pool workers mutate ArrayList without synchronization;
    // updates can be lost or corrupt internal size/array state.
    var report = new ArrayList<PricedOrder>();

    // Performance issue: Blocking remote calls occupy the process-wide common ForkJoinPool. Other
    // parallel streams and CompletableFuture work can starve under latency.
    orders.parallelStream().forEach(order -> {
        try {
            report.add(new PricedOrder(order, priceClient.lookup(order.orderId())));
        // Observability issue: The replacement exception drops the cause and order/customer identity,
        // preventing operators from finding which lookup failed and why.
        } catch (RuntimeException failure) {
            throw new RuntimeException("Pricing failed");
        }
    });
    // Data consistency issue: forEach on a parallel ordered stream does not append in encounter
    // order, yet callers receive this list as if it were deterministic and complete.
    return report;
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | High | `report.add` | Unsynchronized shared ArrayList mutation |
| 2 | Performance issue | High | `parallelStream()` | Blocking I/O runs in the common pool |
| 3 | Observability issue | High | catch block | Failure cause and order context discarded |
| 4 | Data consistency issue | High | parallel `forEach` | Output order and completeness are unreliable |

## Issue details

### Shared mutable stream accumulator

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

ArrayList mutation is not atomic or thread-safe. A pure mapping pipeline produces one result per
input without external mutation. The correct sequential and concurrent implementations both join
immutable per-order results.

**Detection:** Repeated high-volume tests, race tooling and output cardinality checks.

### Blocking common-pool work

**Type:** Performance issue · **Severity:** High · **Difficulty:** Senior

Parallel streams use the shared ForkJoinPool, which is tuned for CPU work rather than threads parked
on remote I/O. The correct API accepts an explicitly sized executor whose capacity and ownership are
visible.

**Trade-off:** The caller must size, monitor and close the executor; sequential execution is often
simpler for small batches.

**Detection:** Thread dumps and common-pool queued/active metrics during client latency.

### Failure context discarded

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

The new RuntimeException keeps neither its cause nor order identity. `OrderPricingException`
retains both without logging customer secrets.

**Detection:** Failure-injection test and inspection of exception cause/fields.

### Parallel output contract is unstable

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel `forEach` does not preserve encounter-order side effects, and races can lose rows. Joining
a future list in input order makes the contract deterministic.

**Trade-off:** Preserving order can delay emission behind an earlier slow item; streaming APIs may
choose explicit unordered semantics instead.

**Detection:** Assert cardinality, identity and order under blocked/reordered completions.

## Correct implementation

Package: `lab.corejava.streamprocessing`

- `src/main/java/lab/corejava/streamprocessing/OrderReportService.java`
- `docs/topics/core-java/solutions.md#deterministic-order-reporting`
