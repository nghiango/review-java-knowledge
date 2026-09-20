package lab.springboot.questions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

public class Q05ConfigurationPropertiesBindingExample {

    @ConfigurationProperties(prefix = "database.pool")
    @Validated
    public record ConnectionPoolProperties(
            @NotBlank String driver, @Min(1) int maxConnections, long connectionTimeoutMs) {

        public ConnectionPoolProperties {
            if (connectionTimeoutMs <= 0) {
                connectionTimeoutMs = 30000L;
            }
        }
    }

    public static void main(String[] args) {
        ConnectionPoolProperties props =
                new ConnectionPoolProperties("org.postgresql.Driver", 20, 0L);

        int maxConnections = props.maxConnections(); // 20
        long timeout = props.connectionTimeoutMs(); // 30000 (defaulted)
        boolean isConfigured = maxConnections > 10; // true

        System.out.println(
                "Max connections: "
                        + maxConnections
                        + ", timeout: "
                        + timeout
                        + ", valid: "
                        + isConfigured);
    }
}
