package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q28: Why is the Delayed Message Exchange plugin preferred over dead-letter TTL queue chaining for scheduled delivery?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q28DelayedExchangeVsTtlChaining {

    public static void main(String[] args) {
        // Approach A: Dead-Letter TTL Queue Chaining (Dead-Letter Hack):
        // Publish message with per-message TTL (e.g. expiration = 10000ms) to a temporary queue with no consumers.
        // On TTL expiration, the message routes to DLX -> target consumer queue.
        // Fatal Flaw (Head-of-Line Blocking):
        // In RabbitMQ FIFO queues, TTL is evaluated ONLY when a message reaches the HEAD of the queue!
        // If Message 1 has TTL=60s and Message 2 has TTL=5s: Message 2 WILL NOT EXPIRE until Message 1 expires!
        // This causes severe delay violations when message TTLs are variable.

        // Approach B: RabbitMQ Delayed Message Exchange Plugin (x-delayed-message):
        // Exchange stores delayed messages in an internal Mnesia database table.
        // A dedicated internal timer dispatches each message to the target queue when its individual
        // 'x-delay' milliseconds elapsed, completely eliminating Head-of-Line blocking!

        Map<String, String> approaches =
                Map.of(
                        "TTL Chaining", "Suffers from Head-of-Line blocking with variable per-message TTLs",
                        "Delayed Exchange", "Internal Mnesia timer dispatch; arbitrary delays without HoL blocking");

        boolean pluginEliminatesHeadOfLineBlocking =
                approaches.get("Delayed Exchange").contains("without HoL blocking"); // true
    }
}
