package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q14: Compare the architectural trade-offs: RabbitMQ (smart broker / dumb consumer) vs Kafka (dumb
 * broker / smart consumer).
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q14RabbitMqVsKafkaArchitecture {

    public static void main(String[] args) {
        // RabbitMQ (Smart Broker, Dumb Consumer):
        // Broker maintains complex routing topologies, message states, delivery tags, and purges on
        // ack.
        // Consumers simply connect and receive messages.
        // Best for: Granular routing, task queues, RPC, per-message TTL/priority, low-latency push
        // (~1ms).
        Map<String, String> rabbitmqStrengths =
                Map.of(
                        "Routing", "Rich exchange bindings (wildcards, headers, fanout)",
                        "AckModel", "Per-message acknowledgment and immediate queue deletion",
                        "Latency", "Sub-millisecond direct delivery");

        // Kafka (Dumb Broker, Smart Consumer):
        // Broker is an append-only sequential commit log; does not track individual message acks.
        // Consumers maintain their own offset positions and can replay historical events.
        // Best for: Massive throughput (millions/sec), event sourcing, stream processing, long
        // retention.
        Map<String, String> kafkaStrengths =
                Map.of(
                        "Throughput", "Millions of records/sec via batching & zero-copy page cache",
                        "Retention", "Time/size retention allowing consumers to replay history",
                        "Ordering", "Strict per-partition ordering guaranteed");

        boolean rabbitMqPurgesOnAck =
                rabbitmqStrengths.get("AckModel").contains("deletion"); // true
        boolean kafkaRetainsForReplay = kafkaStrengths.get("Retention").contains("replay"); // true
    }
}
