package lab.rabbitmq.questions;

/** Q09: How should consumers detect redelivered messages and guarantee idempotent processing? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q09ConsumerIdempotencyPatterns {

    public static void main(String[] args) {
        // Redelivered Header:
        // RabbitMQ sets amqp_redelivered = true on the basic.deliver frame when a message was
        // previously
        // delivered to a consumer that crashed or disconnected before acknowledging.
        boolean isRedelivered = true;

        // However, amqp_redelivered == true does NOT prove whether the previous consumer executed
        // business logic before crashing!
        // Therefore, checking amqp_redelivered is NOT enough for idempotency.
        boolean redeliveredHeaderInsufficientForIdempotency = true; // true

        // Reliable Idempotency:
        // 1. Unique Business / Message ID (e.g. idempotency key in message header or body)
        // 2. Atomic test-and-set in deduplication store (RDBMS unique constraint or Redis SETNX)
        boolean atomicDeduplicationRequired = true; // true
    }
}
