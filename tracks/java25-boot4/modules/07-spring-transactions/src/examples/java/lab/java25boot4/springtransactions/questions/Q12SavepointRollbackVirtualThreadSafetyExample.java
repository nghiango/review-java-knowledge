package lab.java25boot4.springtransactions.questions;

public class Q12SavepointRollbackVirtualThreadSafetyExample {

    public static void main(String[] args) {
        // Savepoint rollbacks apply strictly within the thread owning the transaction resource.
        // Virtual threads cannot share or manipulate savepoints created on sibling threads.
        boolean threadBoundSavepoints = true;
        System.out.println(
                "Savepoints are thread-bound: "
                        + threadBoundSavepoints); // Savepoints are thread-bound: true
    }
}
