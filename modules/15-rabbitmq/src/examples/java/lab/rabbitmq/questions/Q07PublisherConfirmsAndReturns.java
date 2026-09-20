package lab.rabbitmq.questions;

import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * Q07: How do publisher confirms and return callbacks guarantee message arrival and routing in
 * RabbitMQ?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q07PublisherConfirmsAndReturns {

    public static void main(String[] args) {
        // Publisher Confirms (confirm-type: correlated):
        // Broker responds with basic.ack once message is persisted to disk / replicated to quorum
        CorrelationData correlationData = new CorrelationData("msg-100");
        boolean publisherConfirmsPreventLostWrites = true; // true

        // Returns Callback (mandatory = true):
        // If a message reaches an exchange, but the routing key matches ZERO bound queues:
        // When mandatory = true, the broker returns the unroutable message back via basic.return.
        // When mandatory = false, the broker silently drops unroutable messages into the void!
        boolean mandatoryReturnsUnroutableMessages = true; // true
    }
}
