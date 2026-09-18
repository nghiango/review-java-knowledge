package lab.corejava.streamprocessing;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class OrderReportService {
    private final PriceClient client;
    private final Executor executor;

    public OrderReportService(PriceClient client, Executor executor) {
        this.client = Objects.requireNonNull(client, "client");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public List<PricedOrder> generate(List<Order> orders) {
        return List.copyOf(orders).stream().map(this::price).toList();
    }

    public List<PricedOrder> generateConcurrently(List<Order> orders) {
        var futures =
                List.copyOf(orders).stream()
                        .map(order -> CompletableFuture.supplyAsync(() -> price(order), executor))
                        .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private PricedOrder price(Order order) {
        try {
            return new PricedOrder(order, client.lookup(order.orderId()));
        } catch (RuntimeException failure) {
            throw new OrderPricingException(order.orderId(), failure);
        }
    }
}
