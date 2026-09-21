package lab.java25boot4.concurrency.broken.unstructuredconcurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderFulfillmentService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public record FulfillmentResult(String inventoryStatus, String paymentStatus) {}

    public FulfillmentResult fulfillOrder(String orderId) {
        CompletableFuture<String> inventoryFuture = CompletableFuture.supplyAsync(
                () -> reserveInventory(orderId),
                executor
        );

        CompletableFuture<String> paymentFuture = CompletableFuture.supplyAsync(
                () -> processPayment(orderId),
                executor
        );

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
