package lab.springtransactions.broken.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final AuditLogService auditLogService;

    public OrderService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void createOrder(String orderId, double amount) {
        insertOrder(orderId, amount);

        try {
            // Attempting to catch exception from REQUIRES_NEW
            auditLogService.recordAudit(orderId, "ORDER_CREATED");
        } catch (Exception e) {
            // Swallowing exception from inner transaction
            // If propagation was REQUIRED, catching here would cause UnexpectedRollbackException on commit!
            // With REQUIRES_NEW, suspending outer transaction holds outer DB connection while inner acquires second DB connection.
        }
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }
}
