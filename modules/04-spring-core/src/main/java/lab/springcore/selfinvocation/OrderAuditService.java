package lab.springcore.selfinvocation;

import org.springframework.stereotype.Service;

@Service
public class OrderAuditService {

    @Audited(action = "PROCESS_ORDER")
    public void recordAudit(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
    }
}
