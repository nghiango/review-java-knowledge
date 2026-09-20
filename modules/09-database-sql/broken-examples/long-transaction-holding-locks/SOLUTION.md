# Solution: Long Transaction Holding Locks

## Annotated Code

### `InvoiceProcessingService.java`
```java
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

    // Transaction issue: Holding transactional database connection and exclusive row locks across slow remote I/O causes connection pool starvation, lock wait timeouts, and transaction deadlocks
    @Transactional
    public void processInvoice(Long invoiceId) {
        InvoiceRecord invoice =
                jdbcClient
                        .sql("SELECT id, status, amount FROM invoices WHERE id = :id FOR UPDATE")
                        .param("id", invoiceId)
                        .query(InvoiceRecord.class)
                        .single();

        // Slow external tax service or PDF generation performed while holding DB row lock and connection
        generatePdfAndNotifyExternalTaxAuthority(invoice);

        jdbcClient
                .sql("UPDATE invoices SET status = 'PROCESSED' WHERE id = :id")
                .param("id", invoiceId)
                .update();
    }

    private void generatePdfAndNotifyExternalTaxAuthority(InvoiceRecord invoice) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processing interrupted", e);
        }
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Prolonged Row-Lock and Connection Holding | Critical | Concurrency | Holding exclusive row locks (`FOR UPDATE`) and database connections across slow network calls blocks all concurrent queries attempting to read or update the row, rapidly exhausting HikariCP pools and triggering `lock_timeout`. |

---

## Remediation Strategy

1. **State Machine / Two-Phase Status Transition:**
   - **Phase 1 (Short TX):** Transition status from `PENDING` to `PROCESSING` with short atomic lock:
     ```sql
     UPDATE invoices SET status = 'PROCESSING' WHERE id = :id AND status = 'PENDING'
     ```
     Commit transaction and release database connection immediately.
   - **Phase 2 (Non-TX):** Perform slow external I/O (PDF generation, remote API call).
   - **Phase 3 (Short TX):** Transition status to `PROCESSED` (or `FAILED` on error).
