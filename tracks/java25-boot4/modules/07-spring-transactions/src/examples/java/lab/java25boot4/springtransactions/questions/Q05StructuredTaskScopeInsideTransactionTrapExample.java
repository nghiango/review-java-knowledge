package lab.java25boot4.springtransactions.questions;

import java.util.concurrent.StructuredTaskScope;

public class Q05StructuredTaskScopeInsideTransactionTrapExample {

    public static void main(String[] args) throws Exception {
        // Trap: Opening a StructuredTaskScope inside a database transaction
        // Causes forked virtual threads to run outside the transaction boundary!
        try (var scope = StructuredTaskScope.open()) {
            var subtask = scope.fork(() -> "Subtask executed on distinct thread");
            scope.join();
            System.out.println(subtask.get()); // Subtask executed on distinct thread
        }
    }
}
