package lab.architecture.questions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q27: Scenario: Resolving Performance Collapse Caused by Microservice Distributed Joins.
 * Demonstrates an asynchronous CQRS materialized view replacing $N+1$ distributed RPC calls.
 */
public class Q27DistributedJoinResolutionScenarioExample {

    // Materialized view pre-joining Customer and Order details
    public record OrderDetailView(
            String orderId, String customerName, String shippingAddress, double total) {}

    public static class OrderDetailMaterializedViewRepository {
        private final Map<String, OrderDetailView> viewStore = new ConcurrentHashMap<>();

        public void updateView(OrderDetailView view) {
            viewStore.put(view.orderId(), view);
        }

        public OrderDetailView findById(String orderId) {
            return viewStore.get(orderId);
        }
    }

    public static void main(String[] args) {
        OrderDetailMaterializedViewRepository repo = new OrderDetailMaterializedViewRepository();
        repo.updateView(new OrderDetailView("ORD-1", "Alice Smith", "123 Main St", 150.00));

        // Single local in-memory lookup instead of 3 cross-network HTTP joins:
        OrderDetailView view = repo.findById("ORD-1");
        boolean fastLookup = view != null && view.customerName().equals("Alice Smith"); // true

        System.out.println("Q23 view: " + view + ", fastLookup: " + fastLookup);
    }
}
