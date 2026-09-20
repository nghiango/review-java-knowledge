package lab.springboot.questions;

import java.util.Map;

public class Q15ServiceConnectionTestcontainersExample {

    record ServiceConnectionMapping(
            String containerType, Map<String, String> autoMappedProperties) {}

    public static void main(String[] args) {
        // Spring Boot 3.1+ @ServiceConnection automatically extracts connection details without
        // manual @DynamicPropertySource
        ServiceConnectionMapping pgMapping =
                new ServiceConnectionMapping(
                        "PostgreSQLContainer",
                        Map.of(
                                "spring.datasource.url", "jdbc:postgresql://localhost:5432/test",
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test"));

        boolean hasUrl =
                pgMapping.autoMappedProperties().containsKey("spring.datasource.url"); // true
        String mappedUrl =
                pgMapping
                        .autoMappedProperties()
                        .get("spring.datasource.url"); // "jdbc:postgresql://localhost:5432/test"

        System.out.println(
                "ServiceConnection mappings: "
                        + pgMapping.autoMappedProperties().keySet()
                        + ", URL: "
                        + mappedUrl
                        + ", valid: "
                        + hasUrl);
    }
}
