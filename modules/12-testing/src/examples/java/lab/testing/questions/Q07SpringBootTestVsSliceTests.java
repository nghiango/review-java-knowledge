package lab.testing.questions;

import java.util.List;

/**
 * Q07: {@code @SpringBootTest} vs the test slices.
 *
 * <p>A slice is a small, curated auto-configuration: {@code @WebMvcTest} loads the web layer and
 * mocks the service layer, {@code @DataJpaTest} loads JPA plus a DataSource and rolls back,
 * {@code @JsonTest} loads only the Jackson testers. {@code @SpringBootTest} loads the whole
 * application, but its default {@code webEnvironment} is {@code MOCK}: the web layer is loaded and
 * driven through {@code MockMvc}, and no embedded server starts unless {@code RANDOM_PORT} or
 * {@code DEFINED_PORT} is requested. The cost difference below is the reason a suite is built from
 * slices, and Spring's per-context-key cache is the reason slices are cheap only when the test
 * classes share a key.
 */
public class Q07SpringBootTestVsSliceTests {

    /** What a test context actually loads, and what that costs. */
    record TestContext(
            String annotation,
            int beans,
            boolean loadsWebEnvironment,
            boolean startsDatabase,
            int startupMillis) {}

    public static void main(String[] args) {
        // No embedded server starts here: @SpringBootTest defaults to webEnvironment = MOCK, and
        // @WebMvcTest drives the web layer through MockMvc rather than a real connector.
        TestContext full = new TestContext("@SpringBootTest", 320, true, true, 4_200);
        TestContext webSlice = new TestContext("@WebMvcTest", 45, true, false, 900);
        TestContext jpaSlice = new TestContext("@DataJpaTest", 30, false, true, 700);
        TestContext jsonSlice = new TestContext("@JsonTest", 15, false, false, 350);

        List<TestContext> contexts = List.of(full, webSlice, jpaSlice, jsonSlice);

        long databaseContexts = contexts.stream().filter(TestContext::startsDatabase).count(); // 2
        long webContexts = contexts.stream().filter(TestContext::loadsWebEnvironment).count(); // 2
        int fullContextBeans = full.beans(); // 320
        int sliceBeans = webSlice.beans() + jpaSlice.beans() + jsonSlice.beans(); // 90

        // Spring caches one context per context key (configuration + properties + profile), so test
        // classes that share a key pay for the context once.
        List<String> contextKeys =
                List.of("web-mvc", "web-mvc", "web-mvc", "web-mvc", "data-jpa", "data-jpa", "json");
        long testClasses = contextKeys.size(); // 7
        long distinctKeys = contextKeys.stream().distinct().count(); // 3
        long cachedStartupMillis = distinctKeys * webSlice.startupMillis(); // 3 x 900
        long uncachedStartupMillis = testClasses * webSlice.startupMillis(); // 7 x 900
        long fullSuiteMillis = testClasses * full.startupMillis(); // 7 x 4200

        System.out.println("Full context beans: " + fullContextBeans); // Full context beans: 320
        System.out.println("Slice beans: " + sliceBeans); // Slice beans: 90
        System.out.println("Database contexts: " + databaseContexts); // Database contexts: 2
        System.out.println("Web contexts: " + webContexts); // Web contexts: 2
        System.out.println("Distinct keys: " + distinctKeys); // Distinct keys: 3
        System.out.println("Cached: " + cachedStartupMillis + " ms"); // Cached: 2700 ms
        System.out.println("Uncached: " + uncachedStartupMillis + " ms"); // Uncached: 6300 ms
        System.out.println("Full suite: " + fullSuiteMillis + " ms"); // Full suite: 29400 ms
    }
}
