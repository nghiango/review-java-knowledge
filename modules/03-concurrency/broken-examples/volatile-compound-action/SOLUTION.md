# Solution: Volatile used for compound check-then-act stock reservation

## Annotated code

```java
public class ItemStock {
    private final String itemId;

    // Concurrency issue: volatile guarantees visibility and memory ordering, but does NOT guarantee
    // atomicity across compound operations (like check-then-act or decrement).
    private volatile int availableQuantity;

    public ItemStock(String itemId, int initialQuantity) {
        this.itemId = itemId;
        this.availableQuantity = initialQuantity;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}

public class InventoryReservationService {
    // Concurrency issue: Plain HashMap is not thread-safe for concurrent read/write operations.
    private final Map<String, ItemStock> stockByItem = new HashMap<>();

    public void registerItem(String itemId, int initialStock) {
        stockByItem.put(itemId, new ItemStock(itemId, initialStock));
    }

    public boolean reserve(String itemId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }

        ItemStock stock = stockByItem.get(itemId);
        if (stock == null) {
            return false;
        }

        // Concurrency issue: Check-then-act race condition. Multiple concurrent threads can pass the check
        // simultaneously when stock is low, leading to overselling and negative stock balances.
        if (stock.getAvailableQuantity() >= requestedQuantity) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                // Concurrency issue: Swallowing InterruptedException clears the thread's interrupt status
                // without restoring it, preventing cooperative cancellation and graceful shutdown.
            }

            // Data consistency issue: Reads volatile quantity again after delay and writes updated value.
            // Any intervening write by another thread is completely overwritten.
            int updated = stock.getAvailableQuantity() - requestedQuantity;
            stock.setAvailableQuantity(updated);
            return true;
        }

        return false;
    }

    public int getAvailableStock(String itemId) {
        ItemStock stock = stockByItem.get(itemId);
        return stock == null ? 0 : stock.getAvailableQuantity();
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | Critical | `ItemStock.availableQuantity` | `volatile` used where atomic state transitions are required |
| 2 | Concurrency issue | Critical | `InventoryReservationService.reserve()` | Check-then-act race condition causes overselling |
| 3 | Concurrency issue | Medium | `InventoryReservationService.reserve()` | Swallowed `InterruptedException` breaks thread interruption |
| 4 | Concurrency issue | High | `InventoryReservationService.stockByItem` | Unsynchronized `HashMap` accessed concurrently |
| 5 | Data consistency issue | Critical | `InventoryReservationService.reserve()` | Non-atomic write blindly overwrites concurrent reservations |

## Issue details

### Volatile does not provide atomicity for compound actions

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

`volatile` guarantees that any write to a variable is immediately flushed to main memory and establishes a happens-before relationship with subsequent reads by other threads. However, a compound sequence such as:
1. `read availableQuantity`
2. `verify availableQuantity >= requested`
3. `calculate remaining = availableQuantity - requested`
4. `write remaining`

consists of separate operations. If Thread A and Thread B both see `availableQuantity = 10` and both request 8, both pass the check and deduct 8. Thread B's write overwrites Thread A's, resulting in `availableQuantity = 2` instead of rejecting the second order, overselling 6 units.

### Swallowing InterruptedException

**Type:** Concurrency issue · **Severity:** Medium · **Difficulty:** Basic

When `Thread.sleep()` or any blocking API throws `InterruptedException`, the JVM automatically clears the interrupted status flag of the current thread. Catching the exception and ignoring it destroys the cancellation signal sent by thread pools or container shutdown hooks. Always call `Thread.currentThread().interrupt()` or propagate the exception.

## Correct implementation

The production-ready fix lives in `lab.concurrency.volatilecompound`:
- `AtomicInventoryService.java` using `AtomicInteger.updateAndGet()` with CAS retry loops to atomically decrement stock only when sufficient quantity exists.
- `OptimisticStampedInventoryService.java` using `StampedLock` optimistic read validation for high-throughput reads with exclusive write fallback.
