# Solution — Auto-Ack Before Processing Completion

## Annotated code

```java
package lab.rabbitmq.broken.autoack;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmationConsumer {

    private final ExternalPaymentGatewayClient paymentClient;
    private final OrderFulfillmentRepository orderRepository;

    public OrderConfirmationConsumer(
            ExternalPaymentGatewayClient paymentClient,
            OrderFulfillmentRepository orderRepository) {
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = "order.confirmations", ackMode = "MANUAL")
    public void onOrderNotification(
            OrderNotification notification,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // Reliability issue: Premature AMQP basicAck before processing operations finish.
        // Once basicAck is sent, RabbitMQ removes the message from the queue immediately.
        // If the process crashes or an unexpected error occurs during verifyAuthorization() or
        // markConfirmed(), the message is permanently lost, degrading to at-most-once delivery.
        channel.basicAck(deliveryTag, false);

        try {
            paymentClient.verifyAuthorization(notification.orderId(), notification.totalAmount());
            orderRepository.markConfirmed(notification.orderId());
        } catch (Exception ex) {
            // Error handling issue: Catching and swallowing generic Exception prevents dead-letter routing,
            // retry interceptors, or alerting from capturing the unfulfilled order.
            System.err.println("Error processing order confirmation: " + ex.getMessage());
        }
    }

    public interface ExternalPaymentGatewayClient {
        void verifyAuthorization(String orderId, java.math.BigDecimal amount);
    }

    public interface OrderFulfillmentRepository {
        void markConfirmed(String orderId);
    }
}
```

## Issue list

### Reliability issue: Premature AMQP acknowledgment degrades to at-most-once delivery

- **Location:** `OrderConfirmationConsumer.java:27`
- **Description:** `channel.basicAck(deliveryTag, false)` is invoked at the very beginning of the listener method before executing business operations.
- **Impact:** In RabbitMQ, acknowledging a delivery tag instructs the broker to permanently purge the message from queue memory. If the application server suffers an OOM, crash, or deployment termination during the HTTP payment call or database persistence, the message is lost forever without any trace. Customers are charged without their order being marked confirmed.
- **Remediation:** Move `channel.basicAck(deliveryTag, false)` to the end of the method, executing strictly after database persistence succeeds. If an error occurs, reject with `channel.basicNack(deliveryTag, false, false)` to route the message to a Dead Letter Exchange (DLX).

### Error handling issue: Swallowing exceptions hides processing failures

- **Location:** `OrderConfirmationConsumer.java:32-34`
- **Description:** Swallowing `Exception` without rethrowing or nacking silently discards failed business operations.
- **Impact:** Failed orders disappear silently into the logs without triggering automated retries or dead-letter queue inspections.
- **Remediation:** Propagate exceptions to a configured Spring AMQP `ErrorHandler` or explicitly invoke `channel.basicNack(deliveryTag, false, false)` to send the record to a Dead Letter Queue.
