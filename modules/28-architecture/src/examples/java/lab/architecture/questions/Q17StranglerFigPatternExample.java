package lab.architecture.questions;

/**
 * Q17: Decomposing a Monolith to Microservices (Strangler Fig Pattern). Demonstrates routing
 * traffic between legacy monolith and new microservice via edge router.
 */
public class Q17StranglerFigPatternExample {

    public static class StranglerRouter {
        private final boolean routePaymentsToNewService;

        public StranglerRouter(boolean routePaymentsToNewService) {
            this.routePaymentsToNewService = routePaymentsToNewService;
        }

        public String routeRequest(String path) {
            if (path.startsWith("/api/payments") && routePaymentsToNewService) {
                return "NEW_PAYMENT_MICROSERVICE";
            }
            return "LEGACY_MONOLITH";
        }
    }

    public static void main(String[] args) {
        StranglerRouter router = new StranglerRouter(true);

        String paymentTarget =
                router.routeRequest("/api/payments/charge"); // NEW_PAYMENT_MICROSERVICE
        String ordersTarget = router.routeRequest("/api/orders/list"); // LEGACY_MONOLITH

        boolean migrated = paymentTarget.equals("NEW_PAYMENT_MICROSERVICE"); // true
        boolean legacyPreserved = ordersTarget.equals("LEGACY_MONOLITH"); // true

        System.out.println("Q17 migrated: " + migrated + ", legacyPreserved: " + legacyPreserved);
    }
}
