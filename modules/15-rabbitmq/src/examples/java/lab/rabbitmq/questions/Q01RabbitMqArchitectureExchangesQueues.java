package lab.rabbitmq.questions;

import java.util.List;

/**
 * Q01: What is the core AMQP 0-9-1 architecture (Brokers, Virtual Hosts, Exchanges, Queues,
 * Bindings)?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q01RabbitMqArchitectureExchangesQueues {

    public static void main(String[] args) {
        // AMQP 0-9-1 decouples message production from storage:
        // Producers NEVER publish directly to queues; they publish to Exchanges with a routing key.
        boolean producersPublishToExchanges = true; // true

        // Exchanges inspect message attributes (routing key, headers) and route copies to bound
        // queues:
        List<String> amqpComponents = List.of("Exchange", "Binding", "RoutingKey", "Queue");
        boolean decoupledRouting = (amqpComponents.size() == 4); // true

        // Virtual Hosts (vhosts) provide multi-tenant isolation within a single RabbitMQ broker
        // instance
        String defaultVhost = "/";
        boolean vhostsProvideNamespaceIsolation = true; // true

        // Queues buffer messages in memory or on disk until consumers acknowledge them
        boolean messagesRemovedAfterAck = true; // true
    }
}
