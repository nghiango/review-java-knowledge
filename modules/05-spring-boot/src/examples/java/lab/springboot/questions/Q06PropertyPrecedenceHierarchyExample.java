package lab.springboot.questions;

import java.util.Map;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

public class Q06PropertyPrecedenceHierarchyExample {

    public static void main(String[] args) {
        // Property precedence in Spring Boot evaluates property sources in reverse order of
        // addition
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();

        // 1. Lower priority: application.yml
        sources.addLast(
                new MapPropertySource(
                        "applicationConfig", Map.of("app.timeout", "5000", "app.env", "default")));

        // 2. Higher priority: System Environment Variables / CLI args (added first)
        sources.addFirst(new MapPropertySource("systemProperties", Map.of("app.timeout", "10000")));

        String timeout =
                env.getProperty(
                        "app.timeout"); // "10000" (systemProperties overrides applicationConfig)
        String envName = env.getProperty("app.env"); // "default" (falls back to applicationConfig)

        boolean isOverridden = "10000".equals(timeout); // true

        System.out.println(
                "Resolved timeout: "
                        + timeout
                        + ", env: "
                        + envName
                        + ", overridden: "
                        + isOverridden);
    }
}
