package lab.architecture.questions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q14: CQRS Read Model Projections and Eventual Consistency.
 * Demonstrates updating a read projection in response to domain events.
 */
public class Q14CqrsProjectionExample {

    public record ItemSoldEvent(String productId, int quantitySold, int revenue) {}

    // Materialized view / projection optimized for reporting
    public static class ProductSalesProjection {
        private final Map<String, Integer> unitsSold = new ConcurrentHashMap<>();
        private final Map<String, Integer> totalRevenue = new ConcurrentHashMap<>();

        public void on(ItemSoldEvent event) {
            unitsSold.merge(event.productId(), event.quantitySold(), Integer::sum);
            totalRevenue.merge(event.productId(), event.revenue(), Integer::sum);
        }

        public int getTotalUnits(String productId) {
            return unitsSold.getOrDefault(productId, 0);
        }

        public int getTotalRevenue(String productId) {
            return totalRevenue.getOrDefault(productId, 0);
        }
    }

    public static void main(String[] args) {
        ProductSalesProjection projection = new ProductSalesProjection();

        projection.on(new ItemSoldEvent("P1", 2, 40));
        projection.on(new ItemSoldEvent("P1", 3, 60));

        int totalUnits = projection.getTotalUnits("P1"); // 5
        int totalRev = projection.getTotalRevenue("P1"); // 100
        boolean valid = (totalUnits == 5 && totalRev == 100); // true

        System.out.println("Q14 units: " + totalUnits + ", rev: " + totalRev + ", valid: " + valid);
    }
}
