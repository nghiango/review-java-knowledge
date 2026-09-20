package lab.testing.questions;

import java.util.List;
import java.util.stream.Stream;

/**
 * Q11: What a test slice contains, and how the auto-configuration decides it.
 *
 * <p>A slice is {@code @OverrideAutoConfiguration(enabled = false)} plus a curated list of
 * auto-configurations, so a slice is defined as much by what it *leaves out* as by what it loads:
 * {@code @DataJpaTest} loads JPA, a DataSource and a transaction but no MVC, {@code @WebMvcTest}
 * loads MVC and Jackson but no DataSource (its services are replaced with mock beans),
 * {@code @JsonTest} loads only the Jackson testers, {@code @RestClientTest} loads one HTTP client
 * plus a mock server. This module's {@code AccountRepositoryIT} is the {@code @DataJpaTest} row: it
 * adds {@code replace = NONE} to keep the container-backed DataSource and {@code @Import} to pull
 * the service under test into the slice.
 */
public class Q11SliceTestContentsAndAutoConfiguration {

    /** The auto-configurations a slice turns on, and whether it keeps a DataSource. */
    record Slice(String annotation, List<String> autoConfigurations, boolean replacesDataSource) {

        boolean loads(String autoConfiguration) {
            return autoConfigurations.contains(autoConfiguration);
        }
    }

    public static void main(String[] args) {
        Slice jpaSlice =
                new Slice(
                        "@DataJpaTest",
                        List.of(
                                "CacheAutoConfiguration",
                                "DataSourceAutoConfiguration",
                                "HibernateJpaAutoConfiguration",
                                "JpaRepositoriesAutoConfiguration",
                                "TransactionAutoConfiguration",
                                "SqlInitializationAutoConfiguration"),
                        true);

        Slice webSlice =
                new Slice(
                        "@WebMvcTest",
                        List.of(
                                "CacheAutoConfiguration",
                                "JacksonAutoConfiguration",
                                "ValidationAutoConfiguration",
                                "WebMvcAutoConfiguration",
                                "HttpMessageConvertersAutoConfiguration",
                                "ErrorMvcAutoConfiguration"),
                        false);

        Slice jsonSlice =
                new Slice(
                        "@JsonTest",
                        List.of(
                                "CacheAutoConfiguration",
                                "JacksonAutoConfiguration",
                                "JsonTestersAutoConfiguration"),
                        false);

        Slice restClientSlice =
                new Slice(
                        "@RestClientTest",
                        List.of(
                                "CacheAutoConfiguration",
                                "HttpClientAutoConfiguration",
                                "RestClientAutoConfiguration",
                                "WebClientAutoConfiguration",
                                "MockRestServiceServerAutoConfiguration"),
                        false);

        List<Slice> slices = List.of(jpaSlice, webSlice, jsonSlice, restClientSlice);

        int jpaAutoConfigs = jpaSlice.autoConfigurations().size(); // 6
        int webAutoConfigs = webSlice.autoConfigurations().size(); // 6
        long distinctAutoConfigurations = allAutoConfigurations(slices).distinct().count(); // 16
        long dataSourceSlices = slices.stream().filter(Slice::replacesDataSource).count(); // 1
        boolean jpaLoadsRepositories = jpaSlice.loads("JpaRepositoriesAutoConfiguration"); // true
        boolean jpaLoadsMvc = jpaSlice.loads("WebMvcAutoConfiguration"); // false
        boolean webLoadsMvc = webSlice.loads("WebMvcAutoConfiguration"); // true
        boolean webLoadsDataSource = webSlice.loads("DataSourceAutoConfiguration"); // false

        System.out.println("JPA auto-configs: " + jpaAutoConfigs); // JPA auto-configs: 6
        System.out.println("Web auto-configs: " + webAutoConfigs); // Web auto-configs: 6
        System.out.println("Distinct: " + distinctAutoConfigurations); // Distinct: 16
        System.out.println("DataSource slices: " + dataSourceSlices); // DataSource slices: 1
        System.out.println("JPA loads repos: " + jpaLoadsRepositories); // JPA loads repos: true
        System.out.println("JPA loads MVC: " + jpaLoadsMvc); // JPA loads MVC: false
        System.out.println("Web loads MVC: " + webLoadsMvc); // Web loads MVC: true
        System.out.println("Web loads DS: " + webLoadsDataSource); // Web loads DS: false
    }

    /** The auto-configurations every slice in {@code slices} turns on, with duplicates kept. */
    private static Stream<String> allAutoConfigurations(List<Slice> slices) {
        return slices.stream().flatMap(slice -> slice.autoConfigurations().stream());
    }
}
