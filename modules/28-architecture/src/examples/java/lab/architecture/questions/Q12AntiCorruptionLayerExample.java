package lab.architecture.questions;

/**
 * Q12: Bounded Contexts and Context Mapping (Anti-Corruption Layer).
 * Demonstrates translating external/legacy system data structures into clean internal domain concepts.
 */
public class Q12AntiCorruptionLayerExample {

    // Legacy external upstream payload with cryptic naming
    public record LegacyMainframeRecord(String CUST_NO, String STAT_CD, long BAL_CENTS) {}

    // Internal clean domain model
    public record CustomerProfile(String customerId, boolean active, double balanceDollars) {}

    // Anti-Corruption Layer (ACL)
    public static class CustomerAntiCorruptionLayer {
        public static CustomerProfile translate(LegacyMainframeRecord legacy) {
            boolean isActive = "01".equals(legacy.STAT_CD()) || "ACT".equalsIgnoreCase(legacy.STAT_CD());
            double dollars = legacy.BAL_CENTS() / 100.0;
            return new CustomerProfile("CUST-" + legacy.CUST_NO(), isActive, dollars);
        }
    }

    public static void main(String[] args) {
        LegacyMainframeRecord legacy = new LegacyMainframeRecord("9988", "ACT", 12550L);
        CustomerProfile profile = CustomerAntiCorruptionLayer.translate(legacy);

        boolean cleanId = profile.customerId().equals("CUST-9988"); // true
        boolean cleanActive = profile.active(); // true
        boolean correctDollars = (profile.balanceDollars() == 125.50); // true

        System.out.println("Q12 profile: " + profile + ", valid: " + (cleanId && cleanActive && correctDollars));
    }
}
