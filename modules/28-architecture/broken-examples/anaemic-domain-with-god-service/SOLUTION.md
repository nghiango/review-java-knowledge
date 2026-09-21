# Solution: Anaemic Domain Model with God Service

## Annotated Code

### `Order.java`

```java
package lab.architecture.broken.anaemicdomain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Maintainability issue: Anaemic domain model with exposed setters violates encapsulation and leaks invariants
public class Order {

    private Long id;
    private String customerId;
    private String status;

    // Data Consistency issue: Direct list mutation via getItems() bypasses aggregate invariant boundary
    private List<OrderItem> items = new ArrayList<>();

    // Data Consistency issue: Total amount can be set arbitrarily without matching sum of line items
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    // Data Consistency issue: Uncontrolled public setter allows bypassing state machine transitions
    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
```

### `OrderManagementService.java`

```java
package lab.architecture.broken.anaemicdomain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

// Maintainability issue: Procedural god service concentrates domain rules that belong inside the aggregate root
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

        // Maintainability issue: Invariant validation scattered in service logic rather than encapsulated in aggregate
        if ("SHIPPED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot add items to completed order");
        }

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        order.getItems().add(item);

        // Maintainability issue: Procedural total re-computation duplicated across service methods
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

        // Data Consistency issue: Missing lifecycle validation allows cancelling already shipped or settled orders
        order.setStatus("CANCELLED");
    }
}
```

## Issue Catalogue

1. **Anaemic Domain Model Anti-Pattern**:
   - Entities are treated as dumb property bags containing data only, with zero business logic.
   - All logic is concentrated in procedural services, leading to duplication, feature envy, and broken invariants.
2. **Leaked Encapsulation via Getters and Setters**:
   - Calling `order.getItems().add(...)` or `order.setTotalAmount(...)` allows external code to mutate state without recalculating totals or checking quantity constraints.
3. **Inconsistent State Machine Transitions**:
   - `cancelOrder` sets status directly to `CANCELLED` without verifying if the order was already `SHIPPED` or `DELIVERED`, corrupting business state.
4. **Lack of Value Objects**:
   - Using primitive `int` and `BigDecimal` for currency and quantities permits negative quantities, zero amounts, and mixed currencies.

## Correct Implementation

The production-grade solution implements a **DDD Rich Domain Aggregate**:
- Aggregate Root: `lab.architecture.richdomain.Order` encapsulates all invariants (`addItem`, `checkout`, `cancel`).
- Value Objects: `Money` (enforces non-negative values and currency matches), `Quantity`, `OrderId`.
- Unmodifiable Collections: `order.getItems()` returns `Collections.unmodifiableList(...)`, preventing external tampering.
- Thin Application Service: `OrderApplicationService` merely coordinates persistence and transaction boundaries while delegating all decisions to `Order`.
