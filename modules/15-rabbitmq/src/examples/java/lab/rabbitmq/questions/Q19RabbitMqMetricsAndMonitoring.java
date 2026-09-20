package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q19: What are the primary RabbitMQ Prometheus metrics, and what do they reveal about cluster
 * health?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q19RabbitMqMetricsAndMonitoring {

    public static void main(String[] args) {
        // Essential RabbitMQ Prometheus metrics:
        Map<String, String> metrics =
                Map.of(
                        "rabbitmq_queue_messages_ready",
                                "Messages waiting in queue to be delivered to consumers",
                        "rabbitmq_queue_messages_unacked",
                                "Messages delivered to consumers awaiting basic.ack",
                        "rabbitmq_queue_consumer_capacity",
                                "Fraction of time queue can immediately deliver (1.0 = optimal)",
                        "rabbitmq_queue_consumer_utilisation",
                                "Fraction of time consumers are ready to receive messages");

        // High messages_ready with low consumer_utilisation indicates slow downstream processing or
        // prefetch bottleneck
        boolean readyIndicatesBacklog =
                metrics.containsKey("rabbitmq_queue_messages_ready"); // true

        // High messages_unacked indicates long processing durations, unacknowledged message
        // accumulation, or thread stalls
        boolean unackedIndicatesInFlight =
                metrics.containsKey("rabbitmq_queue_messages_unacked"); // true
    }
}
