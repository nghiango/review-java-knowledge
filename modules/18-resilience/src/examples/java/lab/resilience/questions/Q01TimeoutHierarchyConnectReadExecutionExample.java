package lab.resilience.questions;

import java.time.Duration;

public class Q01TimeoutHierarchyConnectReadExecutionExample {

    record TimeoutConfig(Duration connectTimeout, Duration readTimeout, Duration executionTimeout) {
        boolean isValidHierarchy() {
            return executionTimeout.compareTo(readTimeout) >= 0
                    && readTimeout.compareTo(connectTimeout) >= 0;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Connect timeout: TCP handshake / SYN-ACK deadline
        // Read timeout: Maximum inactivity gap between incoming response packets
        // Execution timeout (TimeLimiter): Overall end-to-end deadline for the operation
        TimeoutConfig validConfig =
                new TimeoutConfig(
                        Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofSeconds(3));

        boolean isHierarchyValid = validConfig.isValidHierarchy(); // true
        long connectMs = validConfig.connectTimeout().toMillis(); // 500
        long readMs = validConfig.readTimeout().toMillis(); // 2000
        long execMs = validConfig.executionTimeout().toMillis(); // 3000

        System.out.println("Valid hierarchy: " + isHierarchyValid + ", Exec: " + execMs + "ms");
    }
}
