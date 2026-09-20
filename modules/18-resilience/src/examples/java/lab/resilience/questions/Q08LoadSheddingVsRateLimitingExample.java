package lab.resilience.questions;

public class Q08LoadSheddingVsRateLimitingExample {

    enum RequestPriority {
        CRITICAL_PAYMENT,
        BACKGROUND_RECOMMENDATION
    }

    record ServerHealth(double cpuLoad, boolean isOverloaded) {
        boolean allowRequest(RequestPriority priority) {
            if (!isOverloaded) {
                return true;
            }
            // Load shedding drops low-priority background requests when server is saturated
            return priority == RequestPriority.CRITICAL_PAYMENT;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        ServerHealth saturatedServer = new ServerHealth(0.95, true);

        boolean paymentAdmitted =
                saturatedServer.allowRequest(RequestPriority.CRITICAL_PAYMENT); // true
        boolean recommendationShed =
                !saturatedServer.allowRequest(RequestPriority.BACKGROUND_RECOMMENDATION); // true

        System.out.println(
                "Payment admitted: "
                        + paymentAdmitted
                        + ", Background shed: "
                        + recommendationShed);
    }
}
