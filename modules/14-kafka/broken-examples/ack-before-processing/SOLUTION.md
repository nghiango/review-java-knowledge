# Solution — Acknowledging Offset Before Processing Completion

## Annotated code

```java
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
        // Reliability issue: Premature manual offset acknowledgment before business processing completes.
        // If the process crashes or an unhandled error occurs during downstream HTTP calls or DB saves,
        // Kafka has already committed the offset; the message is permanently lost, degrading to at-most-once delivery.
        if (ack != null) {
            ack.acknowledge();
        }

        try {
            paymentClient.confirmSettlement(notification.paymentId(), notification.amount());
            paymentRepository.saveSettledPayment(notification.paymentId(), notification.status());
        } catch (Exception ex) {
            // Error handling issue: Catching and swallowing generic Exception without propagating to Kafka
            // error handler or triggering a retry/DLT mechanism prevents transient error recovery.
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
```

## Issue list

### Reliability issue: Premature offset acknowledgment degrades to at-most-once delivery

- **Location:** `PaymentNotificationConsumer.java:23-25`
- **Description:** Acknowledging the Kafka message offset (`ack.acknowledge()`) before executing downstream business operations commits the consumer group's partition offset to the cluster.
- **Impact:** If the service crashes, encounters OOM, or restarts while calling `confirmSettlement()` or `saveSettledPayment()`, the message will never be re-delivered on restart. In financial payment flows, this leads to lost settlements and un-reconciled financial discrepancies.
- **Remediation:** Always invoke `ack.acknowledge()` *after* all transactional and external business operations complete successfully. In Spring Kafka, use container factory `AckMode.MANUAL_IMMEDIATE` and invoke `acknowledge()` strictly at the tail of the listener, or rely on `AckMode.BATCH`/`RECORD` with proper Spring error handler propagation.

### Error handling issue: Swallowing exceptions prevents retry and dead-letter routing

- **Location:** `PaymentNotificationConsumer.java:31-33`
- **Description:** The `catch (Exception ex)` block logs to standard error and swallows the failure without re-throwing or notifying the Kafka container.
- **Impact:** Failed messages are treated as successfully handled by the framework. Transient network hiccups with the payment gateway result in silent permanent failures without automated retry or Dead Letter Topic (DLT) audit trails.
- **Remediation:** Remove the catch-and-swallow block. Allow business exceptions to bubble up to Spring Kafka's `DefaultErrorHandler`, configured with bounded retries, exponential backoff, and a dead-letter recoverer.
