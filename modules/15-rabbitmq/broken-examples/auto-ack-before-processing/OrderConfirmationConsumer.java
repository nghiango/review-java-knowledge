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
        channel.basicAck(deliveryTag, false);

        try {
            paymentClient.verifyAuthorization(notification.orderId(), notification.totalAmount());
            orderRepository.markConfirmed(notification.orderId());
        } catch (Exception ex) {
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
