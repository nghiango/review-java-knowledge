package lab.rabbitmq.manualack;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Production-grade RabbitMQ consumer demonstrating safe manual acknowledgment.
 *
 * <p>Invokes {@code channel.basicAck()} strictly after external payment verification and database
 * persistence succeed. If an error occurs, rejects with {@code requeue = false} so the message
 * routes directly to the configured Dead Letter Exchange.
 */
@Component
public class SafeOrderConfirmationConsumer {

    public static final String QUEUE = "order.confirmations";

    private final ExternalPaymentGatewayClient paymentClient;
    private final OrderFulfillmentRepository orderRepository;

    public SafeOrderConfirmationConsumer(
            ExternalPaymentGatewayClient paymentClient,
            OrderFulfillmentRepository orderRepository) {
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = QUEUE, ackMode = "MANUAL")
    public void onOrderNotification(
            OrderNotification notification,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            paymentClient.verifyAuthorization(notification.orderId(), notification.totalAmount());
            orderRepository.markConfirmed(notification.orderId());

            // Acknowledge strictly after business logic completes
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            // Reject without requeueing to route poison pill or fatal error to DLX
            channel.basicNack(deliveryTag, false, false);
            throw new OrderProcessingException(
                    "Failed to confirm order: " + notification.orderId(), ex);
        }
    }

    public interface ExternalPaymentGatewayClient {
        void verifyAuthorization(String orderId, BigDecimal amount);
    }

    public interface OrderFulfillmentRepository {
        void markConfirmed(String orderId);
    }

    public static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
