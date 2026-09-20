package lab.cachingredis.stalecache;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ProductCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        ProductRepository productRepository() {
            return new ProductRepository();
        }

        @Bean
        SafeProductService safeProductService(ProductRepository productRepository) {
            return new SafeProductService(productRepository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(SafeProductService.CACHE_NAME);
        }
    }

    private AnnotationConfigApplicationContext context;
    private SafeProductService service;
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        service = context.getBean(SafeProductService.class);
        repository = context.getBean(ProductRepository.class);
    }

    @Test
    @DisplayName("Read operations populate cache and subsequent reads bypass database")
    void readOperation_populatesCacheAndBypassesDatabase() {
        int initialReads = repository.getReadCount();

        Product first = service.getProduct(1L);
        assertThat(first).isNotNull();
        assertThat(repository.getReadCount()).isEqualTo(initialReads + 1);

        // Second call should hit the cache without incrementing repository read count
        Product cached = service.getProduct(1L);
        assertThat(cached).isEqualTo(first);
        assertThat(repository.getReadCount()).isEqualTo(initialReads + 1);
    }

    @Test
    @DisplayName("Updating entity evicts cache and subsequent read reflects fresh database value")
    void updatePrice_evictsCacheAndEnsuresFreshRead() {
        Product initial = service.getProduct(1L);
        assertThat(initial.price()).isEqualByComparingTo("1200.00");

        // Mutate price
        BigDecimal newPrice = new BigDecimal("1399.99");
        service.updatePrice(1L, newPrice);

        // Reading again must hit the database repository and return fresh price
        Product reloaded = service.getProduct(1L);
        assertThat(reloaded.price()).isEqualByComparingTo("1399.99");
    }

    @Test
    @DisplayName("Deleting entity evicts cache preventing zombie reads")
    void deleteProduct_evictsCachePreventingZombieReads() {
        service.getProduct(2L); // prime cache

        service.deleteProduct(2L);

        Product reloaded = service.getProduct(2L);
        assertThat(reloaded).isNull();
    }
}
