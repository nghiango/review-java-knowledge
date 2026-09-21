package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q22: Saga Pattern: Orchestration vs Choreography. Demonstrates centralized Saga Orchestrator
 * driving a state machine versus decentralized choreographic event publication.
 */
public class Q22SagaOrchestrationVsChoreographyExample {

    // 1. Saga Orchestration: Explicit central coordinator with state machine
    public static class OrchestratedSagaCoordinator {
        private final List<String> stepsExecuted = new ArrayList<>();

        public boolean runWorkflow(boolean simulatePaymentFailure) {
            stepsExecuted.add("ReserveStock");
            if (simulatePaymentFailure) {
                // Orchestrator detects failure and triggers explicit rollback
                stepsExecuted.add("Compensate:ReleaseStock");
                return false;
            }
            stepsExecuted.add("ChargePayment");
            stepsExecuted.add("DispatchOrder");
            return true;
        }

        public List<String> getStepsExecuted() {
            return stepsExecuted;
        }
    }

    public static void main(String[] args) {
        OrchestratedSagaCoordinator coordinator = new OrchestratedSagaCoordinator();
        boolean success = coordinator.runWorkflow(true);

        boolean orchestrationManagedRollback =
                coordinator.getStepsExecuted().contains("Compensate:ReleaseStock"); // true

        System.out.println(
                "Q24 success: " + success + ", rolledBack: " + orchestrationManagedRollback);
    }
}
