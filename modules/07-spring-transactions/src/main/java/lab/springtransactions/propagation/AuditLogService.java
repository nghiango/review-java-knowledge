package lab.springtransactions.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    // Standard REQUIRED propagation participates in caller's transaction without acquiring 2nd DB
    // connection
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordAudit(String orderId, String action) {
        if ("FAIL_AUDIT".equals(action)) {
            throw new IllegalStateException("Audit destination unavailable");
        }
        logs.add(orderId + ":" + action);
    }

    public List<String> getLogs() {
        return List.copyOf(logs);
    }
}
