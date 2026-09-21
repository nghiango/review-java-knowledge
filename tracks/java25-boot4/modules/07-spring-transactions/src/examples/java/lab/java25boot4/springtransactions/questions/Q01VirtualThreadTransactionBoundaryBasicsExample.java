package lab.java25boot4.springtransactions.questions;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class Q01VirtualThreadTransactionBoundaryBasicsExample {

    public static void main(String[] args) {
        // Spring transaction status is stored in ThreadLocal TransactionSynchronizationManager
        boolean isTxActiveOnMain = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println(
                "Main thread active tx: " + isTxActiveOnMain); // Main thread active tx: false

        Thread.ofVirtual()
                .start(
                        () -> {
                            boolean isTxActiveOnVirtual =
                                    TransactionSynchronizationManager.isActualTransactionActive();
                            System.out.println(
                                    "Virtual thread active tx: "
                                            + isTxActiveOnVirtual); // Virtual thread active tx:
                            // false
                        });
    }
}
