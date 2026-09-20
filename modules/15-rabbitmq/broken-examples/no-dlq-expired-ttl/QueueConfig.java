package lab.rabbitmq.broken.nodlq;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    public static final String ALERTS_QUEUE = "ops.alerts.incoming";

    @Bean
    public Queue alertQueue() {
        Map<String, Object> args = new HashMap<>();
        // Set queue message TTL to 60 seconds
        args.put("x-message-ttl", 60000);

        // Queue declared with TTL but without Dead Letter Exchange (x-dead-letter-exchange)
        return new Queue(ALERTS_QUEUE, true, false, false, args);
    }
}
