# Solution — No Dead Letter Queue for Expired and Rejected Messages

## Annotated code

### `QueueConfig.java`

```java
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

        // Reliability issue: Missing Dead Letter Exchange configuration.
        // Without x-dead-letter-exchange and x-dead-letter-routing-key, any message that expires
        // after 60 seconds or is rejected with requeue=false is permanently discarded by RabbitMQ.
        // Critical alerts are lost without an audit log or dead-letter storage.
        return new Queue(ALERTS_QUEUE, true, false, false, args);
    }
}
```

### `ExpiringAlertConsumer.java`

```java
package lab.rabbitmq.broken.nodlq;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ExpiringAlertConsumer {

    private final PagerNotificationClient pagerClient;

    public ExpiringAlertConsumer(PagerNotificationClient pagerClient) {
        this.pagerClient = pagerClient;
    }

    @RabbitListener(queues = QueueConfig.ALERTS_QUEUE, ackMode = "MANUAL")
    public void onAlert(
            AlertNotification alert,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            pagerClient.dispatchToOnCall(alert.alertId(), alert.severity(), alert.message());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            // Reliability issue: Rejecting with requeue=false without DLX silently destroys the message.
            channel.basicReject(deliveryTag, false);
        }
    }

    public interface PagerNotificationClient {
        void dispatchToOnCall(String alertId, String severity, String message);
    }
}
```

## Issue list

### Reliability issue: Missing Dead Letter Exchange silently discards expired and rejected messages

- **Location:** `QueueConfig.java:18-20`, `ExpiringAlertConsumer.java:29`
- **Description:** `ops.alerts.incoming` defines `x-message-ttl` and the consumer rejects failed deliveries with `requeue = false`, but the queue declaration lacks `x-dead-letter-exchange` and `x-dead-letter-routing-key`.
- **Impact:** In RabbitMQ, a dead-letter event occurs under three conditions: (1) message TTL expiration, (2) rejection/nack with `requeue = false`, or (3) queue length limit exceeded (`x-max-length`). If no DLX is bound to the queue, RabbitMQ silently drops the message into the void. In an operational alert pipeline, critical P1 server failure notifications that fail dispatch or expire in queue during a brief consumer outage vanish without audit records, preventing on-call responders from detecting outages.
- **Remediation:** 
  1. Configure `x-dead-letter-exchange` (e.g. `ops.alerts.dlx`) and `x-dead-letter-routing-key` (e.g. `ops.alerts.deadletter`) in queue arguments.
  2. Declare a durable Dead Letter Queue bound to `ops.alerts.dlx` with that routing key, allowing operations teams to monitor DLQ depth and replay failed alerts.
