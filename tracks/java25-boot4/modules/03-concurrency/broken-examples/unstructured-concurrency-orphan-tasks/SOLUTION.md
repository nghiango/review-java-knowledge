# Solution: Unstructured Concurrency & Orphan Task Leaks

## Issues Identified

```java
package lab.java25boot4.concurrency.broken.unstructuredconcurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderFulfillmentService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public record FulfillmentResult(String inventoryStatus, String paymentStatus) {}

    public FulfillmentResult fulfillOrder(String orderId) {
        // Concurrency issue: Unstructured tasks have independent lifecycles; if one fails, sibling tasks are never cancelled
        CompletableFuture<String> inventoryFuture = CompletableFuture.supplyAsync(
                () -> reserveInventory(orderId),
                executor
        );

        CompletableFuture<String> paymentFuture = CompletableFuture.supplyAsync(
                () -> processPayment(orderId),
                executor
        );

        // Error-handling issue: allOf().join() hides cause exceptions inside CompletionException and allows orphan payment task to run to completion even if inventory failed
        CompletableFuture.allOf(inventoryFuture, paymentFuture).join();

        return new FulfillmentResult(inventoryFuture.join(), paymentFuture.join());
    }

    private String reserveInventory(String orderId) {
        if ("order-fail".equals(orderId)) {
            throw new IllegalStateException("Out of stock");
        }
        return "RESERVED";
    }

    private String processPayment(String orderId) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "CHARGED";
    }
}
```

### 1. Concurrency issue (Orphan Tasks & Leakage)
In unstructured concurrency (`CompletableFuture` or bare executors), subtasks do not have a shared parent-child lexical scope. If `reserveInventory` throws an exception, `paymentFuture` continues executing in the background unnoticed. This wastes carrier threads, database connections, and can cause inconsistent state (e.g. charging a customer for out-of-stock items).

### 2. Error-handling issue (Inefficient Cancellation & Aggregated Exceptions)
`CompletableFuture.allOf()` waits for all tasks to complete even if one has already failed. It does not issue interruption signals to running sibling futures.

## Refactored Solution (Java 25 `StructuredTaskScope.ShutdownOnFailure`)
In Java 25:
1. `StructuredTaskScope` treats split subtasks as a single unit of work.
2. `ShutdownOnFailure` automatically cancels (interrupts) all sibling subtasks as soon as any subtask fails.
3. The scope blocks at `.join()` and fails fast via `.throwIfFailed()`.
