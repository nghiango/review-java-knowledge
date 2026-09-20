package lab.cachingredis.stalecache;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicInteger readCount = new AtomicInteger();

    public ProductRepository() {
        store.put(1L, new Product(1L, "Laptop", new BigDecimal("1200.00"), 10));
        store.put(2L, new Product(2L, "Smartphone", new BigDecimal("800.00"), 25));
    }

    public Optional<Product> findById(Long id) {
        readCount.incrementAndGet();
        return Optional.ofNullable(store.get(id));
    }

    public Product save(Product product) {
        store.put(product.id(), product);
        return product;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public int getReadCount() {
        return readCount.get();
    }
}
