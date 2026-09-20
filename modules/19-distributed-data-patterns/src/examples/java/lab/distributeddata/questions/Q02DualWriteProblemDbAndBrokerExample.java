package lab.distributeddata.questions;

public class Q02DualWriteProblemDbAndBrokerExample {

    record DualWriteOutcome(boolean dbCommitted, boolean brokerSent, boolean isConsistent) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Scenario 1: DB committed successfully, but network dropped during broker publish
        DualWriteOutcome scenario1 = new DualWriteOutcome(true, false, false);

        // Scenario 2: Broker send succeeded, but database transaction rolled back
        DualWriteOutcome scenario2 = new DualWriteOutcome(false, true, false);

        boolean s1Inconsistent = !scenario1.isConsistent(); // true
        boolean s2Inconsistent = !scenario2.isConsistent(); // true

        System.out.println(
                "Dual write failure: silent data loss (S1="
                        + s1Inconsistent
                        + "), phantom events (S2="
                        + s2Inconsistent
                        + ")");
    }
}
