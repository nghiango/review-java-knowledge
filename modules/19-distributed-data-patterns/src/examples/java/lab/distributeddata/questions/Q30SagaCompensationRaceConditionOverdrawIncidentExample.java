package lab.distributeddata.questions;

import java.util.Map;

/**
 * Q30: Production Incident: Concurrent Saga execution and out-of-order compensation drained inventory during network partition.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q30SagaCompensationRaceConditionOverdrawIncidentExample {

    public static void main(String[] args) {
        // Incident Scenario:
        // A customer placed an order, and the Saga Coordinator sent 'ReserveInventory(item=101, qty=5)'.
        // Network timeout occurred. The Saga Coordinator assumed failure and initiated compensation:
        // 'ReleaseInventory(item=101, qty=5)'.

        // Race Condition Sequence:
        // 1. Due to network reordering, 'ReleaseInventory' arrived at Inventory Service FIRST!
        //    Because the original reservation hadn't arrived yet, ReleaseInventory failed or credited +5 units!
        // 2. Seconds later, the delayed original 'ReserveInventory' arrived and reserved 5 units.
        // 3. Final state: Inventory counts were corrupted, and customer received free merchandise without payment!

        boolean outOfOrderCompensationCorruptsState = true; // true

        // Remediation:
        // 1. Semantic Locking & State Tracking:
        //    Participants must record Saga execution state per sagaId.
        //    If a compensation event arrives before the forward action, the participant records a 'CANCELLED'
        //    tombstone for that sagaId.
        // 2. When the delayed forward action eventually arrives, it detects the 'CANCELLED' tombstone and
        //    aborts immediately without applying changes.

        Map<String, String> sagaGuarantees =
                Map.of(
                        "Naive Saga", "Vulnerable to out-of-order compensation arrival",
                        "Idempotent State-Tracking", "Tombstones prevent delayed forward actions from executing");

        boolean tombstonePreventsRace =
                sagaGuarantees.get("Idempotent State-Tracking").contains("Tombstones prevent"); // true
    }
}
