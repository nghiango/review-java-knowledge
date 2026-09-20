package lab.springtransactions.questions;

public class Q14AsyncMultithreadTransactionBoundaryExample {

    record ThreadTransactionContext(
            String threadName, boolean hasBoundTransaction, boolean isIndependent) {}

    public static void main(String[] args) {
        // @Async executes in a separate thread. TransactionSynchronizationManager uses ThreadLocal,
        // so the calling transaction is NOT inherited by the worker thread.
        ThreadTransactionContext caller = new ThreadTransactionContext("http-exec-1", true, false);
        ThreadTransactionContext asyncWorker =
                new ThreadTransactionContext("async-pool-4", false, true);

        boolean callerHasTx = caller.hasBoundTransaction(); // true
        boolean asyncHasNoTx = !asyncWorker.hasBoundTransaction(); // true
        boolean isDecoupled = asyncWorker.isIndependent(); // true

        System.out.println(
                "Caller has TX: "
                        + callerHasTx
                        + ", Async worker has no TX: "
                        + asyncHasNoTx
                        + ", independent: "
                        + isDecoupled);
    }
}
