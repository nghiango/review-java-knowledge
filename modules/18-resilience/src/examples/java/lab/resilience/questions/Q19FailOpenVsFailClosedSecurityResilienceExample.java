package lab.resilience.questions;

public class Q19FailOpenVsFailClosedSecurityResilienceExample {

    enum DecisionMode {
        FAIL_OPEN,
        FAIL_CLOSED
    }

    record AccessControl(DecisionMode mode) {
        boolean evaluateAccessWhenServiceFails() {
            // Security authorization MUST fail-closed (deny access)
            // Recommendation widget can fail-open (show default items)
            return mode == DecisionMode.FAIL_OPEN;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        AccessControl securityGate = new AccessControl(DecisionMode.FAIL_CLOSED);
        AccessControl recommendations = new AccessControl(DecisionMode.FAIL_OPEN);

        boolean authBlocksOnFailure = !securityGate.evaluateAccessWhenServiceFails(); // true
        boolean uiPermitsOnFailure = recommendations.evaluateAccessWhenServiceFails(); // true

        System.out.println(
                "Security fails closed: "
                        + authBlocksOnFailure
                        + ", UI fails open: "
                        + uiPermitsOnFailure);
    }
}
