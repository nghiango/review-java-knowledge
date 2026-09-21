package lab.architecture.questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Q04: Aggregate and Aggregate Root Boundaries. Demonstrates how Aggregate Root controls access to
 * internal entities.
 */
public class Q04AggregateRootBoundaryExample {

    public static class OrderItem {
        private final String productId;
        private final int quantity;

        public OrderItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    // Aggregate Root
    public static class OrderAggregate {
        private final String id;
        private final List<OrderItem> items = new ArrayList<>();

        public OrderAggregate(String id) {
            this.id = id;
        }

        // External callers cannot mutate items directly; must pass through Root
        public void addItem(String productId, int quantity) {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            items.add(new OrderItem(productId, quantity));
        }

        public List<OrderItem> getItems() {
            return Collections.unmodifiableList(items); // Enforces root encapsulation
        }
    }

    public static void main(String[] args) {
        OrderAggregate root = new OrderAggregate("ORD-100");
        root.addItem("SKU-A", 3);

        int count = root.getItems().size(); // 1
        boolean isProtected = false;
        try {
            root.getItems().clear();
        } catch (UnsupportedOperationException e) {
            isProtected = true; // true (unmodifiable list protects invariant)
        }

        System.out.println("Q04 count: " + count + ", isProtected: " + isProtected);
    }
}
