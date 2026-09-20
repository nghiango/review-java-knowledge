package lab.cachingredis.broken.stalecache;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final Map<Long, Product> database = new ConcurrentHashMap<>();

    public ProductService() {
        database.put(1L, new Product(1L, "Laptop", new BigDecimal("1200.00"), 10));
        database.put(2L, new Product(2L, "Smartphone", new BigDecimal("800.00"), 25));
    }

    @Cacheable(value = "products", key = "#id")
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }

    public Product updatePrice(Long id, BigDecimal newPrice) {
        Product existing = database.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        Product updated = new Product(existing.id(), existing.name(), newPrice, existing.stockQuantity());
        database.put(id, updated);
        return updated;
    }

    public void deleteProduct(Long id) {
        database.remove(id);
    }
}
