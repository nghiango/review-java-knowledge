package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q28: Scenario: Designing a Resilient Distributed Workflow with Compensating Actions. Demonstrates
 * 4-step distributed transaction where Step 3 fails and triggers compensating rollbacks.
 */
public class Q28SagaCompensatingTransactionScenarioExample {

    public static class StepResult {
        private final boolean success;
        private final String message;

        public StepResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ResilientOrderSaga {
        private final List<String> auditLog = new ArrayList<>();
        private int reservedStock = 0;

        public StepResult executeWorkflow(boolean paymentGatewayUp) {
            // Step 1: Validate Order
            auditLog.add("STEP_1_VALIDATED");

            // Step 2: Reserve Inventory
            reservedStock += 2;
            auditLog.add("STEP_2_INVENTORY_RESERVED");

            // Step 3: Charge Payment (External Gateway)
            if (!paymentGatewayUp) {
                // Failure detected! Trigger compensation
                auditLog.add("STEP_3_PAYMENT_FAILED");
                compensateInventoryReservation();
                auditLog.add("COMPENSATION_INVENTORY_RESTOCKED");
                return new StepResult(false, "Payment failed; inventory compensation executed");
            }

            // Step 4: Dispatch
            auditLog.add("STEP_4_DISPATCHED");
            return new StepResult(true, "Order completed successfully");
        }

        private void compensateInventoryReservation() {
            reservedStock -= 2;
        }

        public int getReservedStock() {
            return reservedStock;
        }

        public List<String> getAuditLog() {
            return auditLog;
        }
    }

    public static void main(String[] args) {
        ResilientOrderSaga saga = new ResilientOrderSaga();
        StepResult result = saga.executeWorkflow(false);

        boolean inventoryRestocked = (saga.getReservedStock() == 0); // true (rollback successful)
        boolean auditComplete =
                saga.getAuditLog().contains("COMPENSATION_INVENTORY_RESTOCKED"); // true

        System.out.println(
                "Q28 result: "
                        + result.getMessage()
                        + ", restocked: "
                        + inventoryRestocked
                        + ", audit: "
                        + auditComplete);
    }
}
