package lab.concurrency.broken.asyncpipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CustomerDashboardService {
    private final PricingClient pricingClient;
    private final OrderHistoryClient orderHistoryClient;

    public CustomerDashboardService(PricingClient pricingClient, OrderHistoryClient orderHistoryClient) {
        this.pricingClient = pricingClient;
        this.orderHistoryClient = orderHistoryClient;
    }

    public Map<String, Object> buildDashboard(String customerId, List<String> itemIds) {
        Map<String, Object> result = new HashMap<>();

        // Blocking on common ForkJoinPool without dedicated executor or error handling
        CompletableFuture<Integer> orderCountFuture =
                CompletableFuture.supplyAsync(() -> orderHistoryClient.fetchOrderCount(customerId));

        List<Double> prices = new ArrayList<>();
        for (String itemId : itemIds) {
            // Anti-pattern: CompletableFuture created and immediately joined inside loop iteration,
            // serializing all calls and blocking caller thread repeatedly.
            Double price =
                    CompletableFuture.supplyAsync(() -> pricingClient.fetchPrice(itemId)).join();
            prices.add(price);
        }

        // Blocking join on unmanaged future without explicit timeout
        Integer totalOrders = orderCountFuture.join();

        result.put("customerId", customerId);
        result.put("totalOrders", totalOrders);
        result.put("prices", prices);
        return result;
    }
}
