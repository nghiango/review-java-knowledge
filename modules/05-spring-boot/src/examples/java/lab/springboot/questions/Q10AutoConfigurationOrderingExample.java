package lab.springboot.questions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.Ordered;

public class Q10AutoConfigurationOrderingExample {

    @AutoConfiguration
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    static class SecurityAutoConfiguration {}

    @AutoConfiguration
    @AutoConfigureAfter(SecurityAutoConfiguration.class)
    static class WebMvcAutoConfiguration {}

    @AutoConfiguration
    @AutoConfigureBefore(WebMvcAutoConfiguration.class)
    static class CorsAutoConfiguration {}

    public static void main(String[] args) {
        // Auto-configuration classes use sorting annotations rather than @Order
        boolean isSecurityHighPriority =
                SecurityAutoConfiguration.class.isAnnotationPresent(
                        AutoConfigureOrder.class); // true
        boolean isWebMvcAfterSecurity =
                WebMvcAutoConfiguration.class.isAnnotationPresent(AutoConfigureAfter.class); // true

        System.out.println(
                "Security ordering: "
                        + isSecurityHighPriority
                        + ", web mvc sequencing: "
                        + isWebMvcAfterSecurity);
    }
}
