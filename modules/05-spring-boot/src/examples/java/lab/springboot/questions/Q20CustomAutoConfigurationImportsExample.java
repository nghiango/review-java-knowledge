package lab.springboot.questions;

import java.util.List;

public class Q20CustomAutoConfigurationImportsExample {

    record ImportsRegistry(String location, List<String> autoConfigurationClasses) {}

    public static void main(String[] args) {
        // In Spring Boot 3.x, auto-configurations are registered in
        // META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        // instead of legacy META-INF/spring.factories
        ImportsRegistry imports =
                new ImportsRegistry(
                        "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                        List.of(
                                "lab.springboot.autoconfigoverride.PaymentClientAutoConfiguration",
                                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"));

        boolean isModernImportsPath = imports.location().endsWith(".imports"); // true
        int registeredCount = imports.autoConfigurationClasses().size(); // 2

        System.out.println(
                "Imports path: "
                        + imports.location()
                        + ", registered auto-configs: "
                        + registeredCount
                        + ", valid: "
                        + isModernImportsPath);
    }
}
