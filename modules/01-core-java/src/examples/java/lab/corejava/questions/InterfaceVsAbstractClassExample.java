package lab.corejava.questions;

public class InterfaceVsAbstractClassExample {
    public record PaymentResult() {}
    public record PaymentRequest() {}

    public static class AuditService {
        public void record(PaymentRequest request) {};
    }
    // Interface
    public interface PaymentProcessor {
        PaymentResult process(PaymentResult result);
    }

    // Abstract
    public abstract static class BasePaymentProcessor
            implements PaymentProcessor {

        private final AuditService auditService;

        protected BasePaymentProcessor(AuditService auditService) {
            this.auditService = auditService;
        }

        protected final void audit(PaymentRequest request) {
            auditService.record(request);
        }
    }
}
