package lab.springtransactions.questions;

public class Q18DistributedSagaVsTwoPhaseCommitExample {

    record DistributedTxModel(
            String model, boolean holdsGlobalLocks, boolean usesCompensatingTransactions) {}

    public static void main(String[] args) {
        // Two-Phase Commit (XA/2PC): Holds locks across all participants until coordinator commits
        // (high latency, low availability)
        DistributedTxModel xa2pc = new DistributedTxModel("2PC", true, false);

        // Saga Pattern: Sequence of local transactions with compensating undo actions (eventual
        // consistency, high throughput)
        DistributedTxModel saga = new DistributedTxModel("Saga", false, true);

        boolean xaHoldsLocks = xa2pc.holdsGlobalLocks(); // true
        boolean sagaUsesCompensations = saga.usesCompensatingTransactions(); // true

        System.out.println(
                "2PC holds locks: "
                        + xaHoldsLocks
                        + ", Saga uses compensating actions: "
                        + sagaUsesCompensations);
    }
}
