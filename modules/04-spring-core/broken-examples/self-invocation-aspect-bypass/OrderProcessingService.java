package lab.springcore.broken.selfinvocation;

import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    public void processOrder(String orderId) {
        // Business logic
        System.out.println("Processing order: " + orderId);

        // Anti-pattern: Calling another annotated method on 'this' within the same class
        auditOrder(orderId);
    }

    @Audited(action = "PROCESS_ORDER")
    public void auditOrder(String orderId) {
        System.out.println("Executing audit record for: " + orderId);
    }
}
