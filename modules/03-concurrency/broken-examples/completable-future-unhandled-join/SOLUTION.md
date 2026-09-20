# Solution: CompletableFuture synchronous join in loops and common-pool starvation

## Annotated code

```java
public class CustomerDashboardService {
    private final PricingClient pricingClient;
    private final OrderHistoryClient orderHistoryClient;

    public CustomerDashboardService(PricingClient pricingClient, OrderHistoryClient orderHistoryClient) {
        this.pricingClient = pricingClient;
        this.orderHistoryClient = orderHistoryClient;
    }

    public Map<String, Object> buildDashboard(String customerId, List<String> itemIds) {
        Map<String, Object> result = new HashMap<>();

        // Performance issue: supplyAsync without an explicit Executor uses ForkJoinPool.commonPool().
        // Executing blocking I/O on commonPool starvations parallel streams and other CPU tasks JVM-wide.
        // Reliability issue: Missing exception handling (exceptionally / handle); an error crashes the entire request.
        CompletableFuture<Integer> orderCountFuture =
                CompletableFuture.supplyAsync(() -> orderHistoryClient.fetchOrderCount(customerId));

        List<Double> prices = new ArrayList<>();
        for (String itemId : itemIds) {
            // Concurrency issue: Invoking .join() on each future sequentially inside a loop serializes
            // network requests, eliminating concurrency and multiplying total latency by N.
            // Reliability issue: Missing explicit timeout (orTimeout); stalls in pricingClient hang indefinitely.
            Double price =
                    CompletableFuture.supplyAsync(() -> pricingClient.fetchPrice(itemId)).join();
            prices.add(price);
        }

        // Reliability issue: Unbounded join() blocks the thread without a timeout deadline.
        Integer totalOrders = orderCountFuture.join();

        result.put("customerId", customerId);
        result.put("totalOrders", totalOrders);
        result.put("prices", prices);
        return result;
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | Critical | `CustomerDashboardService.buildDashboard()` | `.join()` inside loop serializes asynchronous execution |
| 2 | Performance issue | Critical | `CustomerDashboardService.buildDashboard()` | Blocking I/O executed on `ForkJoinPool.commonPool()` |
| 3 | Reliability issue | High | `CustomerDashboardService.buildDashboard()` | Missing `.exceptionally()` / fallback error handling |
| 4 | Reliability issue | High | `CustomerDashboardService.buildDashboard()` | Missing bounded timeouts (`orTimeout`) on futures |

## Issue details

### Serialized asynchronous execution via `.join()` inside loop

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

When `CompletableFuture.supplyAsync(...).join()` is called inside each iteration of a loop, the loop creates a background task and immediately halts waiting for it to complete before starting the next iteration. If 10 items each take 100ms, the total time is 1,000ms instead of 100ms.

Fix: Spawn all futures concurrently first (collecting into a `List<CompletableFuture<Double>>`), combine them with `CompletableFuture.allOf(...)`, and extract completed values asynchronously.

### Blocking common ForkJoinPool

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Senior

`ForkJoinPool.commonPool()` is configured with parallelism proportional to CPU cores (`Runtime.getRuntime().availableProcessors() - 1`). It is intended for compute-intensive tasks (such as `Arrays.parallelSort` and parallel streams). Squeezing blocking HTTP/DB calls into `commonPool` saturates all carrier threads, causing parallel streams and other tasks across the entire JVM to freeze.

Fix: Always supply a dedicated bounded `ExecutorService` (or virtual thread executor) tailored for I/O workloads when invoking `CompletableFuture.supplyAsync(task, ioExecutor)`.

### Missing timeouts and resilient error fallbacks

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

If an external service is slow or unresponsive, an unmanaged `join()` blocks caller threads indefinitely, exhausting application server worker pools. Use Java 9+ `.orTimeout(timeout, unit)` or `.completeOnTimeout(defaultValue, timeout, unit)` along with `.exceptionally(ex -> fallbackValue)` for graceful degradation.

## Correct implementation

The production-ready fix lives in `lab.concurrency.asyncpipeline`:
- `AsyncCustomerDashboardService.java` configuring a dedicated bounded I/O executor, non-blocking `allOf` fan-out, timeouts via `orTimeout`, and resilient partial fallbacks.
- `CustomerDashboard.java` typed immutable response record.
