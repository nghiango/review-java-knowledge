# Solution: recent order summaries

## Annotated code

```java
package lab.performance.broken.batchloading;

import java.util.List;

public final class OrderSummaryService {
    private final OrderRepository orders;
    private final CustomerRepository customers;

    public OrderSummaryService(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public List<OrderSummary> recentOrders(int limit) {
        // API design issue: Caller-controlled limit has no upper bound.
        return orders.findRecent(limit).stream()
                .map(order ->
                        // Database issue: One customer query runs for every order (N+1).
                        // Performance issue: Duplicate customer IDs repeat identical I/O.
                        new OrderSummary(order.id(), customers.findById(order.customerId()).name()))
                .toList();
    }

    public record Order(long id, long customerId) {}

    public record Customer(long id, String name) {}

    public record OrderSummary(long orderId, String customerName) {}

    public interface OrderRepository {
        List<Order> findRecent(int limit);
    }

    public interface CustomerRepository {
        Customer findById(long id);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | API design issue | Medium | `recentOrders()` | Result limit is unbounded |
| 2 | Database issue | High | mapping lambda | N+1 customer queries |
| 3 | Performance issue | High | mapping lambda | Duplicate keys repeat I/O |

## Issue details

### Unbounded result limit
**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic  
**Technology:** pagination · **Interview frequency:** Medium · **Production impact:** Medium

**Problem:** callers choose arbitrary work size. **Why it happens:** the limit is passed through.
**Production impact:** one request can monopolize DB and heap. **Correct implementation:** validate
a service maximum. **Trade-offs:** clients paginate. **How to detect it:** correlate requested
limit with latency and allocation. **Interview follow-up:** Offset or keyset pagination?

### N+1 customer queries
**Type:** Database issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** SQL, repositories · **Interview frequency:** High · **Production impact:** High

**Problem:** N orders cause N customer lookups after the first query. **Why it happens:** I/O is
hidden inside mapping. **Production impact:** latency and pool demand grow linearly. **Correct
implementation:** collect IDs and fetch them in one bounded query. **Trade-offs:** mapping and
missing-row handling become explicit. **How to detect it:** SQL statement counts and traces.
**Interview follow-up:** When is a join preferable?

### Duplicate keys repeat I/O
**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** batching · **Interview frequency:** Medium · **Production impact:** Medium

**Problem:** orders for one customer repeat the same query. **Why it happens:** keys are not
deduplicated. **Production impact:** avoidable round trips. **Correct implementation:** use an
ordered set before batch loading. **Trade-offs:** small temporary collection. **How to detect it:**
group query parameters in traces. **Interview follow-up:** When would request-scoped caching help?

## Correct implementation

See package `lab.performance.batchloading` and
[solutions](../../../docs/topics/performance/solutions.md#bounded-batch-loading).
