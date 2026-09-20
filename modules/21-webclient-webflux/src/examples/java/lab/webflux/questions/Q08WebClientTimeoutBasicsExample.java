package lab.webflux.questions;

import java.time.Duration;

public class Q08WebClientTimeoutBasicsExample {

    record WebClientTimeoutConfig(Duration responseTimeout, Duration connectionTimeout) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // WebClient requires two timeout configurations:
        // 1. Connection timeout: Time allowed to establish TCP/TLS connection (e.g. 500ms).
        // 2. Response timeout: Time allowed to receive complete HTTP response bytes from downstream
        // (e.g. 3000ms).
        WebClientTimeoutConfig config =
                new WebClientTimeoutConfig(Duration.ofMillis(3000), Duration.ofMillis(500));

        long responseTimeoutMs = config.responseTimeout().toMillis(); // 3000L
        long connectTimeoutMs = config.connectionTimeout().toMillis(); // 500L

        System.out.println("Configured response timeout ms: " + responseTimeoutMs);
        System.out.println("Configured connection timeout ms: " + connectTimeoutMs);
    }
}
