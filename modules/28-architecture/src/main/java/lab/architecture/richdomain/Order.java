package lab.architecture.richdomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * DDD Rich Domain Aggregate Root.
 * Fully encapsulates business invariants, state transitions, and total calculation.
 */
public class Order {

    public static final int MAX_ITEMS_LIMIT = 50;

    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;
    private Money totalAmount;

    public Order(OrderId id, String customerId) {
        this.id = Objects.requireNonNull(id, "OrderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId must not be null");
        this.status = OrderStatus.DRAFT;
        this.totalAmount = Money.ZERO;
    }

    public void addOrUpdateItem(String productId, Quantity quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify order in status: " + status);
        }

        Optional<OrderItem> existing = items.stream()
                .filter(it -> it.productId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            OrderItem current = existing.get();
            items.remove(current);
            Quantity newQty = Quantity.of(current.quantity().value() + quantity.value());
            items.add(new OrderItem(productId, newQty, unitPrice));
        } else {
            if (items.size() >= MAX_ITEMS_LIMIT) {
                throw new IllegalStateException("Order cannot contain more than " + MAX_ITEMS_LIMIT + " distinct items");
            }
            items.add(new OrderItem(productId, quantity, unitPrice));
        }

        recalculateTotal();
    }

    public void submit() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order can only be submitted from DRAFT status, current: " + status);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot submit an empty order");
        }
        if (!totalAmount.isPositive()) {
            throw new IllegalStateException("Cannot submit an order with zero or negative total");
        }
        this.status = OrderStatus.SUBMITTED;
    }

    public void markPaid() {
        if (status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Order must be SUBMITTED before payment, current: " + status);
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel(String reason) {
        Objects.requireNonNull(reason, "Cancellation reason must not be null");
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void recalculateTotal() {
        Money sum = Money.ZERO;
        for (OrderItem item : items) {
            sum = sum.add(item.subtotal());
        }
        this.totalAmount = sum;
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }
}
