package lab.kafka.questions;

import java.util.Map;

/**
 * Q16: When should you choose Kafka Streams over standard Spring Kafka consumers, and how do KTable
 * / RocksDB work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q16KafkaStreamsVsStandardConsumer {

    public static void main(String[] args) {
        // Standard Spring Kafka Consumer:
        // Ideal for event-driven message dispatching, microservice command routing, point-to-point
        // asynchronous tasks
        boolean standardForMicroserviceDispatch = true; // true

        // Kafka Streams:
        // Client library for stream processing with built-in stateful aggregations (windowing,
        // joins, tumbling windows)
        // Backed by embedded RocksDB state stores and backed up to internal Kafka changelog topics
        boolean statefulStreamAnalytics = true; // true

        Map<String, String> streamConcepts =
                Map.of(
                        "KStream", "Continuous record stream representing facts/inserts",
                        "KTable", "Upsert stream representing current state snapshot by key",
                        "GlobalKTable",
                                "Fully replicated cache on every instance for broadcast joins");

        boolean ktableRepresentsState =
                streamConcepts.get("KTable").contains("state snapshot"); // true
    }
}
