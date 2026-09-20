package lab.springboot.questions;

import java.util.List;

public class Q12CustomStarterStructureExample {

    record ModuleArchitecture(String moduleName, String purpose, List<String> contains) {}

    public static void main(String[] args) {
        // Best practice: separate starter (dependency aggregator) and autoconfigure (code +
        // conditions)
        ModuleArchitecture autoconfigureModule =
                new ModuleArchitecture(
                        "acme-spring-boot-autoconfigure",
                        "Provides @AutoConfiguration classes and condition metadata",
                        List.of(
                                "AcmeAutoConfiguration.class",
                                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));

        ModuleArchitecture starterModule =
                new ModuleArchitecture(
                        "acme-spring-boot-starter",
                        "Aggregates acme-core client library and autoconfigure module",
                        List.of("acme-core.jar", "acme-spring-boot-autoconfigure.jar"));

        boolean hasImportsFile =
                autoconfigureModule.contains().stream()
                        .anyMatch(c -> c.contains("AutoConfiguration.imports")); // true
        boolean starterAggregatesAutoconfigure =
                starterModule.contains().contains("acme-spring-boot-autoconfigure.jar"); // true

        System.out.println(
                "Imports declared: "
                        + hasImportsFile
                        + ", starter aggregates: "
                        + starterAggregatesAutoconfigure);
    }
}
