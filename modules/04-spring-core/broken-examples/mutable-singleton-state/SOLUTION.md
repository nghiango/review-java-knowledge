# Solution: Mutable per-request state in a Spring singleton bean

## Annotated code

```java
@Service
public class DiscountCalculationService {

    // Concurrency / Security issue: Spring @Service beans are singletons by default and shared
    // across all incoming HTTP/request threads. Storing per-request customer state in instance fields
    // creates severe race conditions: concurrent requests overwrite fields, corrupting discount calculations
    // and leaking customer IDs across tenant sessions.
    private String currentCustomerId;
    private double orderTotal;
    private int loyaltyPoints;

    public double calculateDiscount(String customerId, double total, int points) {
        // Concurrency issue: Multiple concurrent threads mutate shared fields simultaneously
        this.currentCustomerId = customerId;
        this.orderTotal = total;
        this.loyaltyPoints = points;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Data consistency issue: computeTierDiscount reads instance field that may have been overwritten
        // by another customer's request during the delay.
        return computeTierDiscount() + computeLoyaltyDiscount();
    }

    private double computeTierDiscount() {
        if (orderTotal > 500) {
            return orderTotal * 0.10;
        }
        return orderTotal * 0.05;
    }

    private double computeLoyaltyDiscount() {
        if (loyaltyPoints > 1000) {
            return 20.0;
        }
        return 5.0;
    }

    public String getCurrentCustomerId() {
        return currentCustomerId;
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | Critical | `DiscountCalculationService` fields | Mutable state in singleton bean creates race conditions |
| 2 | Security issue | High | `DiscountCalculationService.currentCustomerId` | Per-request customer ID leaked across concurrent sessions |
| 3 | Data consistency issue | Critical | `DiscountCalculationService.calculateDiscount()` | Calculation uses corrupted fields overwritten by other threads |
| 4 | Design issue | High | Class design | State stored in service instance instead of method parameters |

## Issue details

### Stateless singleton beans in Spring

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Spring container manages beans with singleton scope by default. A single shared instance is injected everywhere and invoked concurrently by dozens of HTTP server threads.

Any instance field in a singleton bean is shared memory. If a method writes to instance fields based on incoming request data, interleaved thread execution will overwrite those fields. Customer A's total of \$1,000 will be replaced by Customer B's total of \$50 before Customer A's discount calculation completes, giving Customer A the wrong discount and exposing Customer B's ID.

### The Pure Stateless Design Rule
Spring singleton services should be **strictly stateless** or hold only references to immutable dependencies (`final` collaborator beans). All per-request data must be:
1. Passed explicitly as method parameters or immutable Command/DTO records.
2. Kept purely on the thread's local execution stack (local variables).
3. Returned directly in an immutable result record.

## Correct implementation

The production-ready fix lives in `lab.springcore.mutablesingleton`:
- `DiscountRequest.java` immutable request record.
- `DiscountResult.java` immutable result record.
- `DiscountCalculationService.java` pure stateless service without mutable instance state.
