package lab.rabbitmq.questions;

/**
 * Q04: How do basic.ack, basic.nack, basic.reject, and the requeue flag control message lifecycle
 * in RabbitMQ?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q04AckNackRejectRequeueMechanics {

    public static void main(String[] args) {
        // basic.ack(deliveryTag, multiple):
        // Confirms successful processing. RabbitMQ deletes the message from the queue.
        boolean ackPurgesFromQueue = true; // true

        // basic.reject(deliveryTag, requeue):
        // Rejects a single message.
        // If requeue = true: RabbitMQ reinserts message at head of queue for redelivery.
        // If requeue = false: RabbitMQ drops the message or routes to DLX if configured.
        boolean rejectSingleRecord = true; // true

        // basic.nack(deliveryTag, multiple, requeue):
        // Same as reject, but supports batch negative acknowledgment via multiple = true
        boolean nackSupportsBatch = true; // true

        // Poison pill trap: calling nack/reject with requeue = true on deterministic errors
        // causes an immediate 100% CPU infinite redelivery spin loop
        boolean requeueTrueDangerousOnPoisonPill = true; // true
    }
}
