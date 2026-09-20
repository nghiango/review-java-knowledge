package lab.performance.broken.batchloading;

import java.util.List;

public final class OrderSummaryService {
    private final OrderRepository orders;
    private final CustomerRepository customers;

    public OrderSummaryService(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public List<OrderSummary> recentOrders(int limit) {
        return orders.findRecent(limit).stream()
                .map(
                        order ->
                                new OrderSummary(
                                        order.id(), customers.findById(order.customerId()).name()))
                .toList();
    }

    public record Order(long id, long customerId) {}

    public record Customer(long id, String name) {}

    public record OrderSummary(long orderId, String customerName) {}

    public interface OrderRepository {
        List<Order> findRecent(int limit);
    }

    public interface CustomerRepository {
        Customer findById(long id);
    }
}
