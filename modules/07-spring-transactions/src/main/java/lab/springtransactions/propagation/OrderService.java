package lab.springtransactions.propagation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final AuditLogService auditLogService;
    private final Map<String, Double> orderStore = new ConcurrentHashMap<>();

    public OrderService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void createOrder(String orderId, double amount) {
        orderStore.put(orderId, amount);
        auditLogService.recordAudit(orderId, "ORDER_CREATED");
    }

    public Double getOrderAmount(String orderId) {
        return orderStore.get(orderId);
    }
}
