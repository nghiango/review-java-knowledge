package lab.resilience.questions;

public class Q02RetryAmplificationAndStormsExample {

    record ServiceLayer(String name, int retryMultiplier) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In a chain: Client (3 retries) -> Gateway (3 retries) -> Service A (3 retries) -> Service
        // B
        int clientRetries = 3;
        int gatewayRetries = 3;
        int serviceARetries = 3;

        // Total downstream amplification factor = 3 * 3 * 3 = 27 calls per initial user request
        int totalAmplification = clientRetries * gatewayRetries * serviceARetries; // 27
        boolean causesSevereStorm = totalAmplification > 10; // true

        System.out.println(
                "Amplification factor: "
                        + totalAmplification
                        + ", Severe storm risk: "
                        + causesSevereStorm);
    }
}
