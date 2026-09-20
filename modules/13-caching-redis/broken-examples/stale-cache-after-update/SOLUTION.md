# Solution — Stale Cache After Entity Mutation

## Annotated code

```java
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

    // Data consistency issue: Caching Optional directly can cause serialization issues in Redis
    // or cache Optional.empty indefinitely when records do not exist.
    @Cacheable(value = "products", key = "#id")
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }

    // Data consistency issue: Modifying an entity in persistent storage without invalidating or
    // updating the corresponding cache entry leaves stale data in the cache until TTL expiration.
    public Product updatePrice(Long id, BigDecimal newPrice) {
        Product existing = database.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        Product updated = new Product(existing.id(), existing.name(), newPrice, existing.stockQuantity());
        database.put(id, updated);
        return updated;
    }

    // Data consistency issue: Deleting an entity from storage without invalidating the cache
    // allows consumers to continue reading deleted records from the cache (zombie reads).
    public void deleteProduct(Long id) {
        database.remove(id);
    }
}
```

## Issues identified

### 1. Stale cache after mutation
- **Category:** Data consistency issue
- **Severity:** High
- **Explanation:** `updatePrice` updates the database state but lacks `@CacheEvict(value = "products", key = "#id")` or `@CachePut`. Any client querying `findById` after an update will receive stale product pricing indefinitely or until TTL expiration.

### 2. Zombie reads after entity deletion
- **Category:** Data consistency issue
- **Severity:** High
- **Explanation:** `deleteProduct` removes the entity from the database but leaves the cached entry untouched. Clients calling `findById(id)` will continue to observe the deleted product as if it still exists.

### 3. Caching Optional return values
- **Category:** Design issue
- **Severity:** Medium
- **Explanation:** Caching `Optional<Product>` directly in Redis causes serialization issues because `Optional` does not implement `Serializable` and Jackson requires custom modules to deserialize `Optional` fields cleanly. If a record is missing, `Optional.empty` is cached without a distinct sentinel TTL.

## Correct implementation

See `lab.cachingredis.stalecache.SafeProductService` under `src/main/java`.

## Trade-offs

- **Evict vs Put:** `@CacheEvict` forces the next read to fetch fresh state from the database, ensuring correctness with minimal complexity. `@CachePut` immediately updates the cache, saving one subsequent read query, but can cause race conditions if concurrent writes finish out of order.
- **Eviction timing:** Using `beforeInvocation = false` ensures cache eviction occurs only if the database write succeeds, preventing accidental eviction on failed mutations.
