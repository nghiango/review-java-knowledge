package lab.rabbitmq.questions;

/**
 * Q03: What are the differences among durable queues, durable exchanges, and persistent messages
 * (delivery_mode=2)?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q03MessageDurabilityAndPersistence {

    public static void main(String[] args) {
        // Durable Queue: Survives broker restarts (queue metadata re-created upon broker reboot)
        boolean durableQueueSurvivesReboot = true; // true

        // Transient Message in Durable Queue:
        // If message has delivery_mode = 1 (transient), RabbitMQ buffers in memory only.
        // On broker reboot, the queue survives, but all transient messages inside it are LOST!
        int transientDeliveryMode = 1;
        boolean transientLostOnBrokerRestart = true; // true

        // Persistent Message:
        // Message published with delivery_mode = 2 is written to disk log and committed via fsync
        int persistentDeliveryMode = 2;
        boolean persistentSurvivesBrokerRestart = true; // true

        // Full durability requires BOTH: Durable Queue + Persistent Message (delivery_mode = 2)
        boolean fullDurability =
                durableQueueSurvivesReboot && persistentSurvivesBrokerRestart; // true
    }
}
