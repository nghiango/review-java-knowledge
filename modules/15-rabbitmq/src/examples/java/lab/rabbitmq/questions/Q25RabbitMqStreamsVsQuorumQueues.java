package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q25: What are RabbitMQ Stream queues, and how do they differ from AMQP Quorum and Classic queues?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q25RabbitMqStreamsVsQuorumQueues {

    public static void main(String[] args) {
        // RabbitMQ Streams (RabbitMQ 3.9+):
        // An append-only, immutable commit log modeled after Apache Kafka, running natively inside RabbitMQ.

        // Key Architectural Differences:
        // 1. Consumption Model:
        //    - Quorum / Classic Queues: Destructive FIFO. Consuming a message and sending basic.ack
        //      deletes the message from the queue memory/disk.
        //    - Stream Queues: Non-destructive. Messages are retained on disk according to retention policies.
        //      Consumers track an offset pointer and can replay historical events from offset 0 or timestamp.
        // 2. Throughput & Fan-out:
        //    - Stream Queues use a dedicated high-performance binary protocol achieving hundreds of thousands
        //      of messages per second with negligible CPU usage.
        //    - Multiple consumer groups can read the same stream independently without duplicating data into
        //      separate fan-out queues.

        Map<String, String> queueComparison =
                Map.of(
                        "Quorum Queue", "Destructive FIFO; Raft consensus; message deleted on ack",
                        "Stream Queue", "Non-destructive append-only log; offset-based replay; high throughput");

        boolean streamSupportsOffsetReplay =
                queueComparison.get("Stream Queue").contains("offset-based replay"); // true
    }
}
