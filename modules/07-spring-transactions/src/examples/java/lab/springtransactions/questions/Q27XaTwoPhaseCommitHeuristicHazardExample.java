package lab.springtransactions.questions;

@SuppressWarnings("unused")
public final class Q27XaTwoPhaseCommitHeuristicHazardExample {
    private Q27XaTwoPhaseCommitHeuristicHazardExample() {}

    public enum XaPhaseOutcome {
        PREPARED,
        HEURISTIC_COMMIT,
        HEURISTIC_ROLLBACK,
        HEURISTIC_HAZARD
    }

    // Heuristic hazard simulation:
    // In Phase 1 (Prepare), Participant A and Participant B both vote YES and acquire pessimistic locks.
    // In Phase 2 (Commit), the Transaction Coordinator commits Participant A, but a network split
    // prevents the commit message from reaching Participant B.
    // Participant B eventually hits a heuristic timeout and unilaterally rolls back, leaving data
    // in an inconsistent, split-brain state!
    public static class XaCoordinatorSimulation {
        public XaPhaseOutcome resolveSplitBrain(boolean networkPartition) {
            if (networkPartition) {
                // Heuristic Hazard: coordinator cannot determine whether participant committed or rolled back!
                return XaPhaseOutcome.HEURISTIC_HAZARD;
            }
            return XaPhaseOutcome.PREPARED;
        }
    }

    public static void main(String[] args) {
        XaCoordinatorSimulation coordinator = new XaCoordinatorSimulation();
        XaPhaseOutcome outcome = coordinator.resolveSplitBrain(true);
        boolean isHazard = (outcome == XaPhaseOutcome.HEURISTIC_HAZARD); // true

        // Sagas avoid this hazard by replacing atomic distributed locking with asynchronous
        // local transactions coupled with compensating rollback events.
    }
}
