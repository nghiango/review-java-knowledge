package lab.rabbitmq.questions;

import java.util.List;

/**
 * Q10: How do the rabbitmq_delayed_message_exchange plugin and DLX TTL queues implement exponential
 * retry backoff?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q10DelayedMessageExchangeRetry {

    public static void main(String[] args) {
        // Strategy A: rabbitmq-delayed-message-exchange plugin (x-delayed-message)
        // Publisher sets x-delay header in milliseconds (e.g. 5000ms).
        // Exchange buffers the message in an internal Mnesia/Erlang timer table and routes to the
        // queue
        // only after the delay expires.
        boolean pluginEnablesArbitraryDelays = true; // true

        // Strategy B: Dead Letter Exchange + TTL retry queues (standard AMQP without plugins)
        // 1. Consumer nacks with requeue = false -> routes to retry_exchange
        // 2. Bound to retry_queue_5s with x-message-ttl = 5000 and x-dead-letter-exchange =
        // main_exchange
        // 3. When TTL expires, message automatically bounces back to main queue!
        List<String> retrySteps =
                List.of("Nack to DLX", "Wait in TTL queue", "Dead-letter back to main queue");
        boolean nativeAmqpRetryCycle = (retrySteps.size() == 3); // true
    }
}
