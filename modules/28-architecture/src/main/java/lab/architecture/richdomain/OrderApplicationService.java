package lab.architecture.richdomain;

import java.util.Objects;

/**
 * Lean Application Service in DDD. Does NOT hold domain rules. Delegates all invariant decisions to
 * the Order aggregate.
 */
public class OrderApplicationService {

    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository =
                Objects.requireNonNull(orderRepository, "OrderRepository must not be null");
    }

    public OrderId createOrder(String customerId) {
        OrderId orderId = OrderId.generate();
        Order order = new Order(orderId, customerId);
        orderRepository.save(order);
        return orderId;
    }

    public void addItem(OrderId orderId, String productId, Quantity quantity, Money unitPrice) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Order not found: " + orderId));
        order.addOrUpdateItem(productId, quantity, unitPrice);
        orderRepository.save(order);
    }

    public void submitOrder(OrderId orderId) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Order not found: " + orderId));
        order.submit();
        orderRepository.save(order);
    }

    public void cancelOrder(OrderId orderId, String reason) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancel(reason);
        orderRepository.save(order);
    }
}
