package lab.springtransactions.questions;

import java.util.ArrayList;
import java.util.List;

public class Q09TransactionSynchronizationManagerExample {

    record TxHook(String phase, String action) {}

    public static void main(String[] args) {
        // TransactionSynchronizationManager enables registering hooks around commit lifecycle
        List<TxHook> executedHooks = new ArrayList<>();

        executedHooks.add(new TxHook("beforeCommit", "flushAuditBatch"));
        executedHooks.add(new TxHook("afterCommit", "publishKafkaEvent"));
        executedHooks.add(new TxHook("afterCompletion", "releaseMetrics"));

        String afterCommitAction = executedHooks.get(1).action(); // "publishKafkaEvent"
        boolean hasAfterCommit = "afterCommit".equals(executedHooks.get(1).phase()); // true

        System.out.println(
                "After commit action: "
                        + afterCommitAction
                        + ", registered properly: "
                        + hasAfterCommit);
    }
}
