package lab.springtransactions.questions;

public class Q03PropagationRequiredVsRequiresNewExample {

    record TransactionScope(
            String name, String propagation, int activeConnections, boolean suspendsOuter) {}

    public static void main(String[] args) {
        // REQUIRED: Joins existing outer transaction, reuses same DB connection (1 connection)
        TransactionScope required =
                new TransactionScope("ServiceA->ServiceB", "REQUIRED", 1, false);

        // REQUIRES_NEW: Suspends outer transaction, acquires 2nd DB connection (2 connections held
        // concurrently)
        TransactionScope requiresNew =
                new TransactionScope("ServiceA->ServiceB", "REQUIRES_NEW", 2, true);

        boolean requiredReusesConnection = required.activeConnections() == 1; // true
        boolean requiresNewHoldsTwo = requiresNew.activeConnections() == 2; // true
        boolean requiresNewSuspends = requiresNew.suspendsOuter(); // true

        System.out.println(
                "REQUIRED reuses conn: "
                        + requiredReusesConnection
                        + ", REQUIRES_NEW holds 2 conn: "
                        + requiresNewHoldsTwo
                        + ", suspends: "
                        + requiresNewSuspends);
    }
}
