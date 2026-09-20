package lab.springcore.selfinvocation;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    private final OrderAuditService auditService;

    public OrderProcessingService(OrderAuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
    }

    public void processOrder(String orderId) {
        // Business processing logic
        // Delegating across proxy boundary to dedicated collaborator ensures AOP aspect is
        // triggered
        auditService.recordAudit(orderId);
    }
}
