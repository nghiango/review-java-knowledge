package lab.rabbitmq.dlx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

class DeadLetterConfigurationTest {

    private final DeadLetterConfiguration config = new DeadLetterConfiguration();

    @Test
    @DisplayName("Alert queue declares x-dead-letter-exchange, routing key, and TTL arguments")
    void alertQueueWithDlx_declaresCorrectArguments() {
        Queue queue = config.alertQueueWithDlx();

        assertThat(queue.getName()).isEqualTo(DeadLetterConfiguration.ALERTS_QUEUE);
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", DeadLetterConfiguration.DLX_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", DeadLetterConfiguration.DLQ_ROUTING_KEY)
                .containsEntry("x-message-ttl", 60000);
    }

    @Test
    @DisplayName("Dead letter binding connects DLQ to DLX using DLQ routing key")
    void deadLetterBinding_configuresCorrectTopology() {
        DirectExchange exchange = config.deadLetterExchange();
        Queue dlq = config.deadLetterQueue();
        Binding binding = config.deadLetterBinding(dlq, exchange);

        assertThat(exchange.getName()).isEqualTo(DeadLetterConfiguration.DLX_EXCHANGE);
        assertThat(dlq.getName()).isEqualTo(DeadLetterConfiguration.DLQ_QUEUE);
        assertThat(binding.getDestination()).isEqualTo(DeadLetterConfiguration.DLQ_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(DeadLetterConfiguration.DLX_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(DeadLetterConfiguration.DLQ_ROUTING_KEY);
    }
}
