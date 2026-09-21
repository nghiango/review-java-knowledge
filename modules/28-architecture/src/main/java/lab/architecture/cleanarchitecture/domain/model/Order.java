package lab.architecture.cleanarchitecture.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Pure domain Aggregate Root with zero framework or database dependencies. Business invariants are
 * protected internally; outside callers cannot tamper with state.
 */
public class Order {

    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private final Currency currency;
    private OrderStatus status;
    private Money totalAmount;

    public Order(OrderId id, String customerId, Currency currency) {
        this.id = Objects.requireNonNull(id, "OrderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId must not be null");
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
        this.status = OrderStatus.CREATED;
        this.totalAmount = Money.zero(currency);
    }

    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "Item must not be null");
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot add items to order in status: " + status);
        }
        if (!item.unitPrice().currency().equals(this.currency)) {
            throw new IllegalArgumentException(
                    "Item currency "
                            + item.unitPrice().currency()
                            + " does not match order currency "
                            + this.currency);
        }

        this.items.add(item);
        this.totalAmount = this.totalAmount.add(item.subtotal());
    }

    public void markPaid() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Order must be in CREATED status to be marked PAID, current: " + status);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot pay for an order with no items");
        }
        if (!totalAmount.isPositive()) {
            throw new IllegalStateException("Cannot pay for an order with non-positive total");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel(String reason) {
        Objects.requireNonNull(reason, "Cancellation reason must not be null");
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel an order that has already shipped");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Currency getCurrency() {
        return currency;
    }
}
