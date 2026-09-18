package lab.corejava.questions;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public final class Q19ParallelStreamStarvationExample {
    private Q19ParallelStreamStarvationExample() {}

    public static void main(String[] args) {
        // Redesign: Use dedicated bounded thread pool instead of blocking common ForkJoinPool
        ExecutorService pricingPool = Executors.newFixedThreadPool(4);
        try {
            List<String> orderIds = List.of("ord-1", "ord-2", "ord-3");
            var futures =
                    orderIds.stream()
                            .map(
                                    id ->
                                            CompletableFuture.supplyAsync(
                                                    () -> fetchPrice(id), pricingPool))
                            .toList();

            List<Double> prices = futures.stream().map(CompletableFuture::join).toList();
            Double firstPrice = prices.get(0); // 19.99 (common ForkJoinPool remains unblocked)
        } finally {
            pricingPool.shutdown();
        }
    }

    private static Double fetchPrice(String orderId) {
        return 19.99;
    }
}
