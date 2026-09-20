package lab.restapi.safemethods;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    public Order createOrder(String customerId) {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, customerId, OrderStatus.PENDING);
        orders.put(orderId, order);
        return order;
    }

    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order cancelOrder(UUID id) {
        return orders.compute(
                id,
                (key, existing) -> {
                    if (existing == null) {
                        throw new IllegalArgumentException("Order not found: " + id);
                    }
                    if (existing.status() == OrderStatus.CANCELLED) {
                        return existing;
                    }
                    return new Order(existing.id(), existing.customerId(), OrderStatus.CANCELLED);
                });
    }
}
