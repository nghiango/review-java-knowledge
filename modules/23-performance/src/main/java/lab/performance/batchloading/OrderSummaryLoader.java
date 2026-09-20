package lab.performance.batchloading;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class OrderSummaryLoader {
    private final OrderRepository orders;
    private final CustomerRepository customers;

    public OrderSummaryLoader(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public List<OrderSummary> loadRecent(int limit) {
        if (limit <= 0 || limit > 1_000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        List<Order> recent = orders.findRecent(limit);
        // Preserve first-seen order while removing duplicate foreign keys before the batch call.
        var customerIds = new LinkedHashSet<Long>();
        recent.forEach(order -> customerIds.add(order.customerId()));
        Map<Long, Customer> byId = customers.findAllById(customerIds);
        return recent.stream()
                .map(order -> new OrderSummary(order.id(), requireCustomer(byId, order).name()))
                .toList();
    }

    private static Customer requireCustomer(Map<Long, Customer> byId, Order order) {
        Customer customer = byId.get(order.customerId());
        if (customer == null) {
            throw new IllegalStateException("missing customer " + order.customerId());
        }
        return customer;
    }
}
