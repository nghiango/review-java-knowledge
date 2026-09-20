package lab.concurrency.asyncpipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready asynchronous dashboard service using dedicated I/O executor, non-blocking
 * parallel fan-out via {@link CompletableFuture#allOf}, explicit deadlines, and resilient partial
 * degradation.
 */
public class AsyncCustomerDashboardService {
    private static final long DEFAULT_TIMEOUT_SECONDS = 3;
    private final PricingClient pricingClient;
    private final OrderHistoryClient orderHistoryClient;
    private final Executor ioExecutor;

    public AsyncCustomerDashboardService(
            PricingClient pricingClient,
            OrderHistoryClient orderHistoryClient,
            Executor ioExecutor) {
        this.pricingClient =
                Objects.requireNonNull(pricingClient, "pricingClient must not be null");
        this.orderHistoryClient =
                Objects.requireNonNull(orderHistoryClient, "orderHistoryClient must not be null");
        this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor must not be null");
    }

    public CompletableFuture<CustomerDashboard> buildDashboardAsync(
            String customerId, List<String> itemIds) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }

        // Asynchronous order count fetch with timeout and graceful 0-order fallback
        CompletableFuture<Integer> orderCountFuture =
                CompletableFuture.supplyAsync(
                                () -> orderHistoryClient.fetchOrderCount(customerId), ioExecutor)
                        .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> 0);

        // Fan-out: trigger all price requests in parallel on dedicated I/O executor
        Map<String, Double> pricesMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> priceFutures =
                (itemIds == null ? List.<String>of() : itemIds)
                        .stream()
                                .map(
                                        itemId ->
                                                CompletableFuture.supplyAsync(
                                                                () ->
                                                                        pricingClient.fetchPrice(
                                                                                itemId),
                                                                ioExecutor)
                                                        .orTimeout(
                                                                DEFAULT_TIMEOUT_SECONDS,
                                                                TimeUnit.SECONDS)
                                                        .exceptionally(
                                                                ex -> 0.0) // Fallback price on
                                                        // downstream error
                                                        .thenAccept(
                                                                price ->
                                                                        pricesMap.put(
                                                                                itemId, price)))
                                .toList();

        // Combine all futures into a single non-blocking composite completion stage
        CompletableFuture<Void> allPricesStage =
                CompletableFuture.allOf(priceFutures.toArray(new CompletableFuture[0]));

        return CompletableFuture.allOf(orderCountFuture, allPricesStage)
                .thenApply(
                        v ->
                                new CustomerDashboard(
                                        customerId,
                                        orderCountFuture.join(),
                                        Collections.unmodifiableMap(pricesMap)));
    }
}
