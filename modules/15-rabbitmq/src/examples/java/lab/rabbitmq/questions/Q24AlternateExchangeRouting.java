package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q24: How does the Alternate Exchange (AE) pattern capture unroutable messages and prevent silent drops?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q24AlternateExchangeRouting {

    public static void main(String[] args) {
        // Unroutable Message Hazard:
        // By default, if an exchange receives a message whose routing key matches NO bound queues,
        // RabbitMQ silently drops the message into the bit bucket!

        // Solution - Alternate Exchange (AE):
        // Configure the primary exchange with argument: "alternate-exchange": "my-ae-fanout"
        // If a message published to the primary exchange cannot be routed to any queue:
        // 1. RabbitMQ forwards the message to the alternate exchange with original body & headers.
        // 2. The alternate exchange (typically a Fanout exchange) routes it to an unrouted-messages queue
        //    for dead-letter inspection, alerting, or manual review.

        Map<String, String> exchangeArgs =
                Map.of(
                        "alternate-exchange", "unrouted.exchange.fanout",
                        "type", "direct");

        boolean preventsSilentMessageDrops =
                exchangeArgs.containsKey("alternate-exchange"); // true

        // Contrast with 'mandatory=true':
        // 'mandatory=true' returns basic.return frame synchronously to the publisher TCP channel.
        // Alternate Exchange handles unroutable messages broker-side without client callbacks.
        boolean handlesUnroutableMessagesBrokerSide = true; // true
    }
}
