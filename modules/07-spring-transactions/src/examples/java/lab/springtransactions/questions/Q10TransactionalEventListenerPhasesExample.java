package lab.springtransactions.questions;

import org.springframework.transaction.event.TransactionPhase;

public class Q10TransactionalEventListenerPhasesExample {

    public static void main(String[] args) {
        // @TransactionalEventListener phases determine when event listeners execute relative to
        // transaction commit
        TransactionPhase afterCommit = TransactionPhase.AFTER_COMMIT;
        TransactionPhase afterRollback = TransactionPhase.AFTER_ROLLBACK;
        TransactionPhase beforeCommit = TransactionPhase.BEFORE_COMMIT;
        TransactionPhase afterCompletion = TransactionPhase.AFTER_COMPLETION;

        boolean isAfterCommit = afterCommit.name().equals("AFTER_COMMIT"); // true
        boolean isAfterRollback = afterRollback.name().equals("AFTER_ROLLBACK"); // true
        boolean isBeforeCommit = beforeCommit.name().equals("BEFORE_COMMIT"); // true
        boolean isAfterCompletion = afterCompletion.name().equals("AFTER_COMPLETION"); // true
        int totalPhases = TransactionPhase.values().length; // 4

        System.out.println(
                "Phases: afterCommit="
                        + isAfterCommit
                        + ", afterRollback="
                        + isAfterRollback
                        + ", beforeCommit="
                        + isBeforeCommit
                        + ", afterCompletion="
                        + isAfterCompletion
                        + ", total="
                        + totalPhases);
    }
}
