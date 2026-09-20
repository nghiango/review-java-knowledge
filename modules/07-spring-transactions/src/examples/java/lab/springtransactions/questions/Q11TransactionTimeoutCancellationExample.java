package lab.springtransactions.questions;

import org.springframework.transaction.TransactionTimedOutException;

public class Q11TransactionTimeoutCancellationExample {

    record TxExecution(int timeoutSeconds, int elapsedSeconds, boolean timedOut) {}

    public static void main(String[] args) {
        // When transaction exceeds timeout, TransactionTimedOutException is thrown and SQL
        // statement cancelled
        TxExecution execution = new TxExecution(5, 7, true);

        boolean isTimedOut = execution.timedOut(); // true
        int timeoutConfigured = execution.timeoutSeconds(); // 5

        System.out.println(
                "Transaction timed out: "
                        + isTimedOut
                        + ", configured timeout: "
                        + timeoutConfigured
                        + "s, exception: "
                        + TransactionTimedOutException.class.getSimpleName());
    }
}
