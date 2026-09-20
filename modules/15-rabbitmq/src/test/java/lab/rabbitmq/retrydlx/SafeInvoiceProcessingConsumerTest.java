package lab.rabbitmq.retrydlx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SafeInvoiceProcessingConsumerTest {

    @Mock private SafeInvoiceProcessingConsumer.TaxValidationService taxService;

    @Mock private SafeInvoiceProcessingConsumer.InvoiceLedgerRepository ledgerRepository;

    @Mock private SafeInvoiceProcessingConsumer.DeadLetterAuditService auditService;

    @Mock private Channel channel;

    @InjectMocks private SafeInvoiceProcessingConsumer consumer;

    @Test
    @DisplayName("Valid invoice task verifies compliance, records ledger, and acknowledges")
    void onInvoice_validTask_acknowledgesSuccessfully() throws IOException {
        InvoiceTask task = new InvoiceTask("inv-1", "VAT-DE123456", new BigDecimal("500.00"));

        consumer.onInvoice(task, channel, 10L);

        verify(taxService).verifyTaxCompliance("VAT-DE123456", new BigDecimal("500.00"));
        verify(ledgerRepository).recordInvoice("inv-1", new BigDecimal("500.00"));
        verify(channel).basicAck(10L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Invalid tax number rejects with requeue=false to avoid infinite spin loop")
    void onInvoice_invalidTax_nacksWithoutRequeue() throws IOException {
        InvoiceTask task = new InvoiceTask("inv-2", "INVALID-TAX", new BigDecimal("300.00"));

        consumer.onInvoice(task, channel, 11L);

        verify(channel).basicNack(11L, false, false);
        verify(auditService).recordPoisonPill(eq("inv-2"), contains("Invalid VAT"));
        verify(ledgerRepository, never()).recordInvoice(any(), any());
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
