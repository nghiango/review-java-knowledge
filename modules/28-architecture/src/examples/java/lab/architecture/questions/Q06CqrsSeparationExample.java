package lab.architecture.questions;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q06: CQRS (Command Query Responsibility Segregation) Fundamentals. Demonstrates separation of
 * mutating Command side and read-optimized Query projection.
 */
public class Q06CqrsSeparationExample {

    // Command (Write side): Mutates state, enforces invariants
    public record CreateProductCommand(String productId, String name, BigDecimal price) {}

    // Query DTO (Read side): Optimized flat projection for fast reads
    public record ProductSummaryView(String productId, String displayName, String formattedPrice) {}

    public static class ProductWriteRepository {
        private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

        public void handle(CreateProductCommand cmd) {
            prices.put(cmd.productId(), cmd.price());
        }
    }

    public static class ProductReadModel {
        public ProductSummaryView getSummary(String id, BigDecimal price) {
            return new ProductSummaryView(id, "PROD-" + id, "$" + price.toPlainString());
        }
    }

    public static void main(String[] args) {
        CreateProductCommand cmd =
                new CreateProductCommand("101", "Keyboard", new BigDecimal("79.99"));
        ProductReadModel readModel = new ProductReadModel();

        ProductSummaryView view = readModel.getSummary(cmd.productId(), cmd.price());
        boolean hasCorrectPrice = view.formattedPrice().equals("$79.99"); // true

        System.out.println("Q06 view: " + view + ", priceMatches: " + hasCorrectPrice);
    }
}
