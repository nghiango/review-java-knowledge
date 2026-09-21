package lab.java25boot4.concurrency;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Demonstrates structured task fan-out using StructuredTaskScope.open() with
 * Joiner.awaitAllSuccessfulOrThrow(). If any subtask fails, sibling subtasks are cancelled
 * immediately.
 */
public class StructuredConcurrencyOrderAggregator {

    public record OrderSummary(String orderId, String inventoryStatus, int priceCents) {}

    public OrderSummary aggregateOrder(String orderId, boolean failInventory, boolean failPricing)
            throws InterruptedException, Throwable {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<String> inventorySubtask =
                    scope.fork(() -> checkInventory(orderId, failInventory));
            Subtask<Integer> pricingSubtask =
                    scope.fork(() -> calculatePricing(orderId, failPricing));

            scope.join();

            return new OrderSummary(orderId, inventorySubtask.get(), pricingSubtask.get());
        }
    }

    private String checkInventory(String orderId, boolean fail) {
        if (fail) {
            throw new IllegalArgumentException("Inventory check failed for order: " + orderId);
        }
        return "IN_STOCK";
    }

    private int calculatePricing(String orderId, boolean fail) {
        if (fail) {
            throw new IllegalStateException("Pricing engine unavailable for order: " + orderId);
        }
        return 4999;
    }
}
