package lab.springtransactions.broken.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(String orderId, String action) {
        if ("FAIL_AUDIT".equals(action)) {
            throw new RuntimeException("Audit system unreachable");
        }
        // DB insert into audit_log
    }
}
