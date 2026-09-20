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
            // Requeue the message on error
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
