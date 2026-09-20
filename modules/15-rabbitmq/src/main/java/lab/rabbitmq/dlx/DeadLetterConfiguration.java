package lab.rabbitmq.dlx;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Production-grade queue configuration with Dead Letter Exchange (DLX) and Dead Letter Queue (DLQ).
 *
 * <p>Messages that expire past their TTL (60s) or are rejected with {@code requeue = false} are
 * automatically forwarded by RabbitMQ to {@code ops.alerts.dlx} and stored in {@code
 * ops.alerts.dlq}.
 */
@Configuration
public class DeadLetterConfiguration {

    public static final String ALERTS_QUEUE = "ops.alerts.incoming";
    public static final String DLX_EXCHANGE = "ops.alerts.dlx";
    public static final String DLQ_QUEUE = "ops.alerts.dlq";
    public static final String DLQ_ROUTING_KEY = "ops.alerts.deadletter";

    @Bean
    public Queue alertQueueWithDlx() {
        Map<String, Object> args = new HashMap<>();
        // Configure message TTL
        args.put("x-message-ttl", 60000);
        // Configure Dead Letter Exchange and Routing Key
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);

        return new Queue(ALERTS_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_QUEUE, true, false, false);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }
}
