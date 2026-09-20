package lab.distributeddata.questions;

public class Q01DistributedTransactionsTwoPhaseCommitPitfallsExample {

    record CoordinatorState(boolean prepareAllReceived, boolean isBlockingIndefinitely) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In 2PC: Coordinator sends PREPARE to all participants.
        // If the coordinator crashes after all nodes reply PREPARED but before sending COMMIT,
        // all participant nodes hold lock resources and block indefinitely (the 2PC blocking
        // problem).
        CoordinatorState state = new CoordinatorState(true, true);

        boolean preparesSucceeded = state.prepareAllReceived(); // true
        boolean blockedOnCrash = state.isBlockingIndefinitely(); // true

        System.out.println(
                "2PC blocks indefinitely when coordinator crashes during phase 2: "
                        + blockedOnCrash);
    }
}
