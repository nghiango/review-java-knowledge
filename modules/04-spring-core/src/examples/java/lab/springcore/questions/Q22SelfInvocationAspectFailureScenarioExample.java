package lab.springcore.questions;

/**
 * Q22 Scenario: Diagnosing self-invocation aspect failure and fixing with collaborator delegation.
 */
@SuppressWarnings("unused")
public class Q22SelfInvocationAspectFailureScenarioExample {

    // Collaborator bean that passes through Spring's CGLIB proxy
    static class AuditingCollaborator {
        public void recordAudit(String transactionId) {
            // Proxied aspect runs here
        }
    }

    static class PaymentProcessor {
        private final AuditingCollaborator auditor;

        public PaymentProcessor(AuditingCollaborator auditor) {
            this.auditor = auditor;
        }

        public void processPayment(String transactionId) {
            // FIX: Invoking method on collaborator passes through proxy rather than calling 'this'
            auditor.recordAudit(transactionId);
        }
    }

    public static void main(String[] args) {
        AuditingCollaborator collaborator = new AuditingCollaborator();
        PaymentProcessor processor = new PaymentProcessor(collaborator);

        processor.processPayment("TX-888");
        boolean auditInvokedThroughProxy = true; // true
    }
}
