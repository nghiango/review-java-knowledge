package lab.java25boot4.springtransactions.questions;

public class Q04TransactionalEventListenerVirtualThreadExample {

    public static void main(String[] args) {
        // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) ensures
        // async virtual threads only process events after the database commit succeeds.
        boolean safePostCommitProcessing = true;
        System.out.println(
                "Dispatched only after successful commit: "
                        + safePostCommitProcessing); // Dispatched only after successful commit:
        // true
    }
}
