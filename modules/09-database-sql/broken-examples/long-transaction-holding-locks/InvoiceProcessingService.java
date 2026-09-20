package lab.databasesql.broken.transactionscope;

import java.math.BigDecimal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceProcessingService {

    private final JdbcClient jdbcClient;

    public InvoiceProcessingService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record InvoiceRecord(Long id, String status, BigDecimal amount) {}

    @Transactional
    public void processInvoice(Long invoiceId) {
        // Lock invoice row exclusively
        InvoiceRecord invoice =
                jdbcClient
                        .sql("SELECT id, status, amount FROM invoices WHERE id = :id FOR UPDATE")
                        .param("id", invoiceId)
                        .query(InvoiceRecord.class)
                        .single();

        // Slow external tax service or PDF generation performed while holding DB row lock and connection
        generatePdfAndNotifyExternalTaxAuthority(invoice);

        // Update status and commit
        jdbcClient
                .sql("UPDATE invoices SET status = 'PROCESSED' WHERE id = :id")
                .param("id", invoiceId)
                .update();
    }

    private void generatePdfAndNotifyExternalTaxAuthority(InvoiceRecord invoice) {
        try {
            // Simulated slow remote I/O (e.g. 3-5 seconds)
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processing interrupted", e);
        }
    }
}
