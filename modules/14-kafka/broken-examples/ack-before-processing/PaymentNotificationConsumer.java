package lab.kafka.broken.ackbeforeprocessing;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationConsumer {

    private final ExternalPaymentGatewayClient paymentClient;
    private final PaymentRecordRepository paymentRepository;

    public PaymentNotificationConsumer(
            ExternalPaymentGatewayClient paymentClient,
            PaymentRecordRepository paymentRepository) {
        this.paymentClient = paymentClient;
        this.paymentRepository = paymentRepository;
    }

    @KafkaListener(topics = "payment-notifications", groupId = "payment-service-group")
    public void onPaymentNotification(PaymentNotification notification, Acknowledgment ack) {
        if (ack != null) {
            ack.acknowledge();
        }

        try {
            paymentClient.confirmSettlement(notification.paymentId(), notification.amount());
            paymentRepository.saveSettledPayment(notification.paymentId(), notification.status());
        } catch (Exception ex) {
            System.err.println("Failed to process payment notification: " + ex.getMessage());
        }
    }

    public interface ExternalPaymentGatewayClient {
        void confirmSettlement(String paymentId, java.math.BigDecimal amount);
    }

    public interface PaymentRecordRepository {
        void saveSettledPayment(String paymentId, String status);
    }
}
