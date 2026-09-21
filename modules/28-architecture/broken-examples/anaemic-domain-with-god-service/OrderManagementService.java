package lab.architecture.broken.anaemicdomain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderManagementService {

    private final Map<Long, Order> orderDb = new HashMap<>();
    private long idSeq = 1;

    public Order createOrder(String customerId) {
        Order order = new Order();
        order.setId(idSeq++);
        order.setCustomerId(customerId);
        order.setStatus("CREATED");
        order.setTotalAmount(BigDecimal.ZERO);
        orderDb.put(order.getId(), order);
        return order;
    }

    public void addItemToOrder(Long orderId, String productId, int quantity, BigDecimal unitPrice) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        // Invariant check in service instead of aggregate
        if ("SHIPPED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot add items to completed order");
        }

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        order.getItems().add(item);

        // Procedural total re-computation
        BigDecimal newTotal = BigDecimal.ZERO;
        for (OrderItem it : order.getItems()) {
            newTotal = newTotal.add(it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }
        order.setTotalAmount(newTotal);
    }

    public void processCheckout(Long orderId) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Order has no items");
        }

        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Total amount is invalid");
        }

        order.setStatus("PAID");
    }

    public void cancelOrder(Long orderId) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        // Inconsistent invariant check: allows cancelling already shipped orders
        order.setStatus("CANCELLED");
    }
}
