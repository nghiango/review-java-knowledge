package lab.testing.questions;

import java.util.Map;
import java.util.Set;

/**
 * Q26: How does Spring TestContext Framework cache ApplicationContext across test classes, and
 * why does @DirtiesContext destroy CI suite performance?
 */
public class Q26ApplicationContextCachingAndDirtiesContext {

    // Context cache key generated from configuration attributes
    record ContextCacheKey(
            Set<String> locations,
            Set<Class<?>> classes,
            Set<String> activeProfiles,
            Map<String, String> propertyOverrides) {}

    public static void main(String[] args) {
        ContextCacheKey suiteContextA =
                new ContextCacheKey(
                        Set.of(),
                        Set.of(lab.testing.pricing.CheckoutService.class),
                        Set.of("test"),
                        Map.of("spring.datasource.url", "jdbc:postgresql://localhost:5432/test"));

        ContextCacheKey suiteContextB =
                new ContextCacheKey(
                        Set.of(),
                        Set.of(lab.testing.pricing.CheckoutService.class),
                        Set.of("test"),
                        Map.of("spring.datasource.url", "jdbc:postgresql://localhost:5432/test"));

        // Same configuration key -> Spring reuses cached ApplicationContext (0ms startup overhead)
        boolean contextReused = suiteContextA.equals(suiteContextB); // true

        // Annotating a test with @DirtiesContext forces context eviction and full cold restart
        int baseStartupMs = 4500;
        int testClassesCount = 20;

        int totalDurationWithCacheMs = baseStartupMs + (testClassesCount * 50); // ~5,500ms
        int totalDurationWithDirtiesContextMs =
                testClassesCount * baseStartupMs; // ~90,000ms (16x slower!)

        System.out.println("Context Cache Hit: " + contextReused); // true
        System.out.println("Suite Time with Cache (ms): " + totalDurationWithCacheMs); // 5500
        System.out.println("Suite Time with @DirtiesContext (ms): " + totalDurationWithDirtiesContextMs); // 90000
    }
}
