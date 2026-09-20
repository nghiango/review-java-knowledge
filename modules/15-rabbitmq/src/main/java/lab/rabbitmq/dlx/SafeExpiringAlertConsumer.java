package lab.rabbitmq.dlx;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class SafeExpiringAlertConsumer {

    private final PagerNotificationClient pagerClient;

    public SafeExpiringAlertConsumer(PagerNotificationClient pagerClient) {
        this.pagerClient = pagerClient;
    }

    @RabbitListener(queues = DeadLetterConfiguration.ALERTS_QUEUE, ackMode = "MANUAL")
    public void onAlert(
            AlertNotification alert,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            pagerClient.dispatchToOnCall(alert.alertId(), alert.severity(), alert.message());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            // Reject with requeue=false: because queue defines DLX, RabbitMQ routes to
            // ops.alerts.dlq
            channel.basicReject(deliveryTag, false);
        }
    }

    public interface PagerNotificationClient {
        void dispatchToOnCall(String alertId, String severity, String message);
    }
}
