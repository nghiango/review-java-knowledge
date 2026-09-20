package lab.databasesql.transactionscope;

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

    /**
     * Phase 1 (Short Transaction): Acquire lock and mark as PROCESSING atomically. Transaction
     * completes and releases the DB connection immediately.
     */
    @Transactional
    public boolean claimInvoiceForProcessing(Long invoiceId) {
        int updated =
                jdbcClient
                        .sql(
                                "UPDATE invoices "
                                        + "SET status = 'PROCESSING' "
                                        + "WHERE id = :id AND status = 'PENDING'")
                        .param("id", invoiceId)
                        .update();
        return updated > 0;
    }

    /** Phase 3 (Short Transaction): Mark as PROCESSED or FAILED upon completion. */
    @Transactional
    public void completeInvoiceProcessing(Long invoiceId, String finalStatus) {
        jdbcClient
                .sql("UPDATE invoices SET status = :status WHERE id = :id")
                .param("status", finalStatus)
                .param("id", invoiceId)
                .update();
    }

    /** Orchestrator method: Executes slow remote I/O OUTSIDE of database transactions. */
    public void processInvoiceLifecycle(Long invoiceId) {
        boolean claimed = claimInvoiceForProcessing(invoiceId);
        if (!claimed) {
            return;
        }

        try {
            // Non-transactional slow external I/O
            performExternalProcessing();
            completeInvoiceProcessing(invoiceId, "PROCESSED");
        } catch (Exception e) {
            completeInvoiceProcessing(invoiceId, "FAILED");
            throw new IllegalStateException("Invoice processing failed", e);
        }
    }

    protected void performExternalProcessing() {
        // External call executed without holding database connection
    }
}
