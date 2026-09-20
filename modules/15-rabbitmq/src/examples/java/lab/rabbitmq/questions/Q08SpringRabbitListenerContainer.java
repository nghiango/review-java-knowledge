package lab.rabbitmq.questions;

import org.springframework.amqp.core.AcknowledgeMode;

/**
 * Q08: What are the differences between SimpleMessageListenerContainer and
 * DirectMessageListenerContainer?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q08SpringRabbitListenerContainer {

    public static void main(String[] args) {
        // SimpleMessageListenerContainer (SMLC):
        // Spawns dedicated consumer threads managed by Spring; messages are queued in an internal
        // BlockingQueue.
        // Supports dynamic concurrency scaling and batch listeners.
        String defaultContainer = "SimpleMessageListenerContainer";

        // DirectMessageListenerContainer (DMLC):
        // Eliminates internal queue; listener executes directly on RabbitMQ client
        // connection/channel threads.
        // Provides higher throughput and lower latency, but lacks some dynamic features.
        String lowLatencyContainer = "DirectMessageListenerContainer";

        // AcknowledgeMode options:
        // NONE: Broker auto-acks as soon as message is sent over TCP (at-most-once risk)
        // AUTO (default): Container acks if listener returns normally, nacks/requeues if exception
        // thrown
        // MANUAL: Application code calls channel.basicAck() / basicNack() directly
        AcknowledgeMode manualMode = AcknowledgeMode.MANUAL;
        boolean manualGivesApplicationFullControl = (manualMode == AcknowledgeMode.MANUAL); // true
    }
}
