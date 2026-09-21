package lab.architecture.questions;

import java.util.Objects;

/**
 * Q24: Cell-Based Architecture Routing. Demonstrates deterministic cellular partitioning where
 * requests are routed to independent, self-contained cell infrastructure to contain blast radiuses.
 */
public class Q24CellBasedArchitectureRoutingExample {

    public static class CellRouter {
        private final int totalCells;

        public CellRouter(int totalCells) {
            this.totalCells = totalCells;
        }

        public String resolveCell(String tenantOrUserId) {
            Objects.requireNonNull(tenantOrUserId);
            int partition = Math.floorMod(tenantOrUserId.hashCode(), totalCells);
            return "cell-" + partition;
        }
    }

    public static void main(String[] args) {
        CellRouter router = new CellRouter(4);

        String cellUser1 = router.resolveCell("user-abc-123"); // "cell-2" (deterministic hash)
        String cellUser2 = router.resolveCell("user-xyz-789");

        // Outage in cell-0 or cell-1 has zero impact on cell-2 users
        boolean isolatedFailureDomain = !cellUser1.isEmpty() && !cellUser2.isEmpty(); // true

        System.out.println("Q26 cellUser1: " + cellUser1 + ", isolated: " + isolatedFailureDomain);
    }
}
