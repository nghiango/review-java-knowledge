package lab.kafka.manualack;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationConsumerTest {

    @Mock private SafePaymentNotificationConsumer.ExternalPaymentGatewayClient paymentClient;

    @Mock private SafePaymentNotificationConsumer.PaymentRecordRepository paymentRepository;

    @Mock private Acknowledgment acknowledgment;

    @InjectMocks private SafePaymentNotificationConsumer consumer;

    @Test
    @DisplayName("Acknowledges offset strictly after external confirmation and database save")
    void onPaymentNotification_success_acknowledgesAfterOperations() {
        PaymentNotification notification =
                new PaymentNotification(
                        "pay-100", "ord-100", new BigDecimal("150.00"), "COMPLETED");

        consumer.onPaymentNotification(notification, acknowledgment);

        InOrder inOrder = inOrder(paymentClient, paymentRepository, acknowledgment);
        inOrder.verify(paymentClient).confirmSettlement("pay-100", new BigDecimal("150.00"));
        inOrder.verify(paymentRepository).saveSettledPayment("pay-100", "COMPLETED");
        inOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Does not acknowledge offset and throws exception when external gateway fails")
    void onPaymentNotification_gatewayFailure_doesNotAcknowledge() {
        PaymentNotification notification =
                new PaymentNotification(
                        "pay-101", "ord-101", new BigDecimal("250.00"), "COMPLETED");

        doThrow(new RuntimeException("Gateway timeout"))
                .when(paymentClient)
                .confirmSettlement(any(), any());

        assertThatThrownBy(() -> consumer.onPaymentNotification(notification, acknowledgment))
                .isInstanceOf(SafePaymentNotificationConsumer.PaymentProcessingException.class)
                .hasMessageContaining("pay-101");

        verify(paymentRepository, never()).saveSettledPayment(any(), any());
        verify(acknowledgment, never()).acknowledge();
    }
}
