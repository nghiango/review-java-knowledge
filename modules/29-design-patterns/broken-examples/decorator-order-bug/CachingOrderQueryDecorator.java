package lab.designpatterns.broken.decoratororder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingOrderQueryDecorator implements OrderQueryService {

    private final OrderQueryService delegate;
    private final Map<String, OrderSummary> cache = new ConcurrentHashMap<>();

    public CachingOrderQueryDecorator(OrderQueryService delegate) {
        this.delegate = delegate;
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        return cache.computeIfAbsent(orderId, delegate::getOrderSummary);
    }

    public void clear() {
        cache.clear();
    }
}
