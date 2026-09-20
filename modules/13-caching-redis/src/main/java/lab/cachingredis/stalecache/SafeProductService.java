package lab.cachingredis.stalecache;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SafeProductService {

    public static final String CACHE_NAME = "products";

    private final ProductRepository productRepository;

    public SafeProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Cache-Aside: Reads are cached under the entity's primary key.
    // unless = "#result == null" ensures missing records are not cached as permanent hits.
    @Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // Explicit Cache Eviction: Updating the entity must evict the existing cache entry.
    // beforeInvocation = false guarantees eviction happens ONLY if the database write succeeds.
    // We choose CacheEvict over CachePut to prevent race conditions under concurrent updates.
    @CacheEvict(value = CACHE_NAME, key = "#id", beforeInvocation = false)
    public Product updatePrice(Long id, BigDecimal newPrice) {
        Product existing =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Product not found: " + id));

        Product updated =
                new Product(existing.id(), existing.name(), newPrice, existing.stockQuantity());
        return productRepository.save(updated);
    }

    // Zombie Read Prevention: Deleting the entity must evict the cached entry immediately.
    @CacheEvict(value = CACHE_NAME, key = "#id", beforeInvocation = false)
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
}
