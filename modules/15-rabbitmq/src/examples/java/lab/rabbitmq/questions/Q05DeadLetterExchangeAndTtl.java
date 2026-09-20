package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q05: How do x-dead-letter-exchange, x-dead-letter-routing-key, and x-message-ttl route dead
 * letters?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q05DeadLetterExchangeAndTtl {

    public static void main(String[] args) {
        // A message is dead-lettered when:
        // 1. Consumer rejects or nacks with requeue = false
        // 2. Message expires due to per-message or per-queue TTL (x-message-ttl)
        // 3. Queue length limit is exceeded (x-max-length or x-max-length-bytes)
        Map<String, Object> queueArgs =
                Map.of(
                        "x-message-ttl", 60000,
                        "x-dead-letter-exchange", "app.deadletter.exchange",
                        "x-dead-letter-routing-key", "deadletter.orders");

        boolean hasDlxConfigured = queueArgs.containsKey("x-dead-letter-exchange"); // true

        // Without x-dead-letter-exchange, expired or rejected messages are silently purged forever
        boolean silentDropWithoutDlx = true; // true
    }
}
