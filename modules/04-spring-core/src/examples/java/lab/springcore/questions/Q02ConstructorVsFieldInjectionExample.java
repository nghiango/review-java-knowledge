package lab.springcore.questions;

/** Q02: Demonstrates Constructor Injection vs Setter Injection vs Field Injection. */
@SuppressWarnings("unused")
public class Q02ConstructorVsFieldInjectionExample {

    static class PaymentGateway {}

    // 1. Constructor Injection (Preferred: immutable, final, testable)
    static class CheckoutService {
        private final PaymentGateway paymentGateway;

        public CheckoutService(PaymentGateway paymentGateway) {
            this.paymentGateway = paymentGateway;
        }

        public boolean isConfigured() {
            return paymentGateway != null;
        }
    }

    public static void main(String[] args) {
        PaymentGateway gateway = new PaymentGateway();
        CheckoutService service = new CheckoutService(gateway);

        boolean ready = service.isConfigured(); // true
    }
}
