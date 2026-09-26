package lab.restapi.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q30NestedQueryMemorySaturationScenarioExample {
    private Q30NestedQueryMemorySaturationScenarioExample() {}

    public record Warehouse(String code, int quantity) {}

    public record ProductVariant(String sku, List<Warehouse> warehouses) {}

    public record Product(String id, List<ProductVariant> variants) {}

    // Deep nested graph hydration without depth bounding:
    // When a client requests ?expand=products.variants.warehouses, hydration of 1,000 products
    // with 10 variants and 50 warehouses instantiates 500,000 objects in a single HTTP request!
    public static class CatalogServiceSimulator {
        public static List<Product> fetchDeepGraph(
                int productCount, int variantsPerProduct, int warehousesPerVariant) {
            List<Product> products = new ArrayList<>();
            for (int p = 0; p < productCount; p++) {
                List<ProductVariant> variants = new ArrayList<>();
                for (int v = 0; v < variantsPerProduct; v++) {
                    List<Warehouse> warehouses = new ArrayList<>();
                    for (int w = 0; w < warehousesPerVariant; w++) {
                        warehouses.add(new Warehouse("WH-" + w, 100));
                    }
                    variants.add(new ProductVariant("SKU-" + p + "-" + v, warehouses));
                }
                products.add(new Product("PROD-" + p, variants));
            }
            return products;
        }
    }

    public static void main(String[] args) {
        // Safe bounded pagination: limits depth and page size
        List<Product> bounded = CatalogServiceSimulator.fetchDeepGraph(5, 2, 2);
        int totalProducts = bounded.size(); // 5
        boolean isSafe = totalProducts <= 10; // true
    }
}
