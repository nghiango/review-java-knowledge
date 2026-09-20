package lab.distributeddata.questions;

public class Q10ExactlyOnceDeliveryFallaciesAndAtLeastOnceRealityExample {

    record DeliveryModel(String transportGuarantee, boolean requiresDeduplicationAtReceiver) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Across the network, packets can always be duplicated by TCP retries, broker rebalances,
        // or producer retries.
        // True "Exactly-Once" is effectively: At-Least-Once Delivery + Idempotent Processing at
        // Receiver.
        DeliveryModel model = new DeliveryModel("AT_LEAST_ONCE", true);

        boolean networkIsAtLeastOnce = "AT_LEAST_ONCE".equals(model.transportGuarantee()); // true
        boolean requiresReceiverDedup = model.requiresDeduplicationAtReceiver(); // true

        System.out.println(
                "Network guarantee: "
                        + networkIsAtLeastOnce
                        + ", Effective exactly-once requires receiver dedup: "
                        + requiresReceiverDedup);
    }
}
