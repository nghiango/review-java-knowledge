package lab.java25boot4.springtransactions.questions;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class Q08TransactionSynchronizationVirtualThreadExample {

    public static void main(String[] args) {
        // Checking synchronization status
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        System.out.println(
                "Synchronization active on current thread: "
                        + synchronizationActive); // Synchronization active on current thread: false
    }
}
