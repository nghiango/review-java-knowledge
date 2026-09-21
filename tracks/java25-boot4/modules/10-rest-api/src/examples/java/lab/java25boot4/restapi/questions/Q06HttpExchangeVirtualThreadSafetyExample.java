package lab.java25boot4.restapi.questions;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Q06: What are the concurrency considerations and timeout configurations when invoking declarative
 * HTTP interface clients on virtual threads?
 */
public class Q06HttpExchangeVirtualThreadSafetyExample {

    public static SimpleClientHttpRequestFactory configureVirtualThreadSafeFactory(
            Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return factory;
    }

    public static void main(String[] args) {
        var factory =
                configureVirtualThreadSafeFactory(Duration.ofSeconds(1), Duration.ofSeconds(2));

        boolean isThreadVirtual = Thread.currentThread().isVirtual();

        System.out.println("Current thread virtual: " + isThreadVirtual); // false
        System.out.println("Configured factory non-null: " + (factory != null)); // true
    }
}
