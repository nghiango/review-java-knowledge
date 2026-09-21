package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q22: Scenario: Refactoring a 2,000-line Monolithic Payment Processor. Demonstrates breaking down
 * procedural switch blocks into pluggable Strategy beans.
 */
public class Q22ScenarioRefactoringMonolithicPaymentGatewayExample {

    public interface PaymentHandler {
        String handle(String account, double amount);
    }

    public static class PaymentRoutingEngine {
        private final Map<String, PaymentHandler> routes = new HashMap<>();

        public void register(String channel, PaymentHandler handler) {
            routes.put(channel, handler);
        }

        public String execute(String channel, String account, double amount) {
            PaymentHandler handler = routes.get(channel);
            if (handler == null) {
                throw new IllegalArgumentException("Unknown channel: " + channel);
            }
            return handler.handle(account, amount);
        }
    }

    public static void main(String[] args) {
        PaymentRoutingEngine engine = new PaymentRoutingEngine();
        engine.register("STRIPE", (acct, amt) -> "STRIPE_OK:" + amt);
        engine.register("PAYPAL", (acct, amt) -> "PAYPAL_OK:" + amt);

        String stripeResult = engine.execute("STRIPE", "acct_123", 99.50); // "STRIPE_OK:99.5"
        boolean routedSuccessfully = stripeResult.startsWith("STRIPE_OK"); // true

        System.out.println("Q22 result: " + stripeResult + ", success: " + routedSuccessfully);
    }
}
