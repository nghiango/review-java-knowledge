package lab.springtransactions.questions;

public class Q07TransactionTemplateProgrammaticExample {

    record ExecutionResult(String outcome, boolean programmaticSuccess) {}

    public static void main(String[] args) {
        // Programmatic transaction boundary allows tight scoping around DB operations
        boolean executed = true;
        ExecutionResult result = new ExecutionResult("DB_COMMITTED", executed);

        boolean isProgrammatic = result.programmaticSuccess(); // true
        String status = result.outcome(); // "DB_COMMITTED"

        System.out.println(
                "TransactionTemplate outcome: " + status + ", success: " + isProgrammatic);
    }
}
