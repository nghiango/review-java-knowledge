# Solution — Infinite Requeue Poison Pill Loop

## Annotated code

```java
package lab.rabbitmq.broken.infiniterequeue;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class InvoiceProcessingConsumer {

    private final TaxValidationService taxService;
    private final InvoiceLedgerRepository ledgerRepository;

    public InvoiceProcessingConsumer(
            TaxValidationService taxService,
            InvoiceLedgerRepository ledgerRepository) {
        this.taxService = taxService;
        this.ledgerRepository = ledgerRepository;
    }

    @RabbitListener(queues = "invoices.incoming", ackMode = "MANUAL")
    public void onInvoice(
            InvoiceTask task,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (task.taxNumber() == null || !task.taxNumber().startsWith("VAT-")) {
                throw new IllegalArgumentException("Invalid VAT tax identifier: " + task.taxNumber());
            }

            taxService.verifyTaxCompliance(task.taxNumber(), task.amount());
            ledgerRepository.recordInvoice(task.invoiceId(), task.amount());

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            // Reliability issue: Unconditional basicNack with requeue=true.
            // When a deterministic error (such as an invalid VAT format) occurs, RabbitMQ immediately
            // reinserts the message at the head of the queue. The consumer receives the same failing message
            // in sub-millisecond cycles, causing an infinite 100% CPU crash loop and blocking all healthy invoices.
            channel.basicNack(deliveryTag, false, true);
        }
    }

    public interface TaxValidationService {
        void verifyTaxCompliance(String taxNumber, java.math.BigDecimal amount);
    }

    public interface InvoiceLedgerRepository {
        void recordInvoice(String invoiceId, java.math.BigDecimal amount);
    }
}
```

## Issue list

### Reliability issue: Requeueing poison pills creates an infinite 100% CPU crash loop

- **Location:** `InvoiceProcessingConsumer.java:37`
- **Description:** `channel.basicNack(deliveryTag, false, true)` unconditionally requeues any failing message back into the source queue.
- **Impact:** In RabbitMQ, requeued messages are placed back at the head of the queue and redelivered immediately to the first available consumer. When a payload fails deterministically due to data validation (e.g. malformed tax number), the consumer thread enters an infinite spin loop, executing thousands of iterations per second. This saturates the CPU, floods logs with gigabytes of identical stack traces, and starves all other pending valid invoices waiting in the queue.
- **Remediation:** 
  1. For deterministic non-retryable errors, reject with `requeue = false` via `channel.basicNack(deliveryTag, false, false)` so RabbitMQ routes the message to the Dead Letter Exchange (DLX).
  2. For transient failures, track delivery counts (using the `x-delivery-count` header on Quorum Queues or Spring AMQP's `RetryOperationsInterceptor`) and reject to DLQ once the maximum retry threshold (e.g. 3 attempts) is exceeded.
