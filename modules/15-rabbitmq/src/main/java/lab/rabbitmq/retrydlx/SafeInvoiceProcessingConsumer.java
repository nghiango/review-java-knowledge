package lab.rabbitmq.retrydlx;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Production-grade consumer protecting against infinite requeue loops.
 *
 * <p>Deterministic data validation errors reject immediately with {@code requeue = false}, allowing
 * RabbitMQ to move the poison message to a Dead Letter Exchange without spinning at 100% CPU.
 */
@Component
public class SafeInvoiceProcessingConsumer {

    public static final String QUEUE = "invoices.incoming";

    private final TaxValidationService taxService;
    private final InvoiceLedgerRepository ledgerRepository;
    private final DeadLetterAuditService auditService;

    public SafeInvoiceProcessingConsumer(
            TaxValidationService taxService,
            InvoiceLedgerRepository ledgerRepository,
            DeadLetterAuditService auditService) {
        this.taxService = taxService;
        this.ledgerRepository = ledgerRepository;
        this.auditService = auditService;
    }

    @RabbitListener(queues = QUEUE, ackMode = "MANUAL")
    public void onInvoice(
            InvoiceTask task, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            if (task.taxNumber() == null || !task.taxNumber().startsWith("VAT-")) {
                // Reject without requeue for deterministic validation error
                channel.basicNack(deliveryTag, false, false);
                auditService.recordPoisonPill(task.invoiceId(), "Invalid VAT: " + task.taxNumber());
                return;
            }

            taxService.verifyTaxCompliance(task.taxNumber(), task.amount());
            ledgerRepository.recordInvoice(task.invoiceId(), task.amount());

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            // Unhandled transient error: reject without requeue to route to DLX after logging
            channel.basicNack(deliveryTag, false, false);
            auditService.recordPoisonPill(task.invoiceId(), ex.getMessage());
        }
    }

    public interface TaxValidationService {
        void verifyTaxCompliance(String taxNumber, BigDecimal amount);
    }

    public interface InvoiceLedgerRepository {
        void recordInvoice(String invoiceId, BigDecimal amount);
    }

    public interface DeadLetterAuditService {
        void recordPoisonPill(String invoiceId, String reason);
    }
}
