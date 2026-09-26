package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q29: Production Incident: Ingestion froze as RabbitMQ blocked all TCP publisher sockets under memory alarm.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29PublisherBlockedMemoryAlarmIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A high-throughput order ingestion pipeline crashed during Black Friday.
        // All Spring Boot order microservices reported hanging threads in rabbitTemplate.convertAndSend().
        // API gateways timed out waiting for responses, causing a complete system outage.

        // Failure Mechanism:
        // 1. A downstream payment worker pool developed a deadlock, stopping acknowledgments.
        // 2. Unacknowledged messages accumulated in RAM, pushing RabbitMQ Erlang process memory past
        //    the vm_memory_high_watermark (40% RAM = 12.8GB of 32GB).
        // 3. RabbitMQ's flow control subsystem tripped the cluster memory alarm.
        // 4. RabbitMQ stopped reading from all publisher TCP sockets, causing TCP window buffer exhaustion.
        // 5. Publisher threads blocked in socket write operations, cascading into thread pool starvation across
        //    all 30 microservices.

        boolean memoryAlarmTripsPublisherBlock = true; // true

        // Remediation:
        // 1. In Spring AMQP, register a BlockedListener to detect 'connection.blocked' and fail fast
        //    or circuit break instead of hanging HTTP threads.
        // 2. Enforce prefetch limits (basic.qos) on consumers to prevent massive unacked queues.
        // 3. Configure queue paging to disk ('x-max-in-memory-length' or Quorum Queues).

        Map<String, String> defense =
                Map.of(
                        "BlockedListener", "Notifies client when connection.blocked frame received",
                        "Quorum Queues", "Efficient segment-based disk offloading; avoids memory leaks");

        boolean detectsBlockedConnection =
                defense.containsKey("BlockedListener"); // true
    }
}
