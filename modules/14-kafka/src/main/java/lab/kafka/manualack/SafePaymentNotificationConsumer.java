package lab.kafka.manualack;

import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Production-grade Kafka consumer demonstrating safe manual offset acknowledgment.
 *
 * <p>Offset commit occurs strictly AFTER external gateway confirmation and database persistence
 * succeed. If an unhandled failure or crash occurs during processing, the offset is not
 * acknowledged, ensuring Kafka redelivers the message to another consumer instance (at-least-once
 * guarantee).
 */
@Component
public class SafePaymentNotificationConsumer {

    public static final String TOPIC = "payment-notifications";
    public static final String GROUP_ID = "payment-service-group";

    private final ExternalPaymentGatewayClient paymentClient;
    private final PaymentRecordRepository paymentRepository;

    public SafePaymentNotificationConsumer(
            ExternalPaymentGatewayClient paymentClient, PaymentRecordRepository paymentRepository) {
        this.paymentClient = paymentClient;
        this.paymentRepository = paymentRepository;
    }

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            containerFactory = "manualAckKafkaListenerContainerFactory")
    public void onPaymentNotification(PaymentNotification notification, Acknowledgment ack) {
        try {
            paymentClient.confirmSettlement(notification.paymentId(), notification.amount());
            paymentRepository.saveSettledPayment(notification.paymentId(), notification.status());

            // Acknowledge offset strictly AFTER successful completion of business operations
            if (ack != null) {
                ack.acknowledge();
            }
        } catch (Exception ex) {
            // Do NOT acknowledge the offset; rethrow so Spring Kafka's error handler can
            // backoff/retry
            throw new PaymentProcessingException(
                    "Failed to process payment settlement for: " + notification.paymentId(), ex);
        }
    }

    public interface ExternalPaymentGatewayClient {
        void confirmSettlement(String paymentId, BigDecimal amount);
    }

    public interface PaymentRecordRepository {
        void saveSettledPayment(String paymentId, String status);
    }

    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
