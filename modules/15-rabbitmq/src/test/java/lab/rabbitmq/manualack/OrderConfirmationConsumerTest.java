package lab.rabbitmq.manualack;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderConfirmationConsumerTest {

    @Mock private SafeOrderConfirmationConsumer.ExternalPaymentGatewayClient paymentClient;

    @Mock private SafeOrderConfirmationConsumer.OrderFulfillmentRepository orderRepository;

    @Mock private Channel channel;

    @InjectMocks private SafeOrderConfirmationConsumer consumer;

    @Test
    @DisplayName("Acknowledges delivery tag strictly after payment check and order save")
    void onOrderNotification_success_acknowledgesAfterOperations() throws IOException {
        OrderNotification notification =
                new OrderNotification("ord-101", "cust-101", new BigDecimal("120.00"), "COMPLETED");

        consumer.onOrderNotification(notification, channel, 42L);

        InOrder inOrder = inOrder(paymentClient, orderRepository, channel);
        inOrder.verify(paymentClient).verifyAuthorization("ord-101", new BigDecimal("120.00"));
        inOrder.verify(orderRepository).markConfirmed("ord-101");
        inOrder.verify(channel).basicAck(42L, false);
    }

    @Test
    @DisplayName("Nacks with requeue=false and rethrows exception when downstream call fails")
    void onOrderNotification_failure_nacksWithoutRequeue() throws IOException {
        OrderNotification notification =
                new OrderNotification("ord-102", "cust-102", new BigDecimal("50.00"), "PENDING");

        doThrow(new RuntimeException("Payment timeout"))
                .when(paymentClient)
                .verifyAuthorization(any(), any());

        assertThatThrownBy(() -> consumer.onOrderNotification(notification, channel, 43L))
                .isInstanceOf(SafeOrderConfirmationConsumer.OrderProcessingException.class)
                .hasMessageContaining("ord-102");

        verify(channel).basicNack(43L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
