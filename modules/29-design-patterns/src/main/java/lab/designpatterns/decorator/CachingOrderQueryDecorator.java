package lab.designpatterns.decorator;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Decorator adding in-memory caching to order queries. */
public class CachingOrderQueryDecorator implements OrderQueryPort {

    private final OrderQueryPort delegate;
    private final Map<String, OrderSummary> cache = new ConcurrentHashMap<>();

    public CachingOrderQueryDecorator(OrderQueryPort delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate must not be null");
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        return cache.computeIfAbsent(orderId, delegate::getOrderSummary);
    }

    public void clear() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }
}
