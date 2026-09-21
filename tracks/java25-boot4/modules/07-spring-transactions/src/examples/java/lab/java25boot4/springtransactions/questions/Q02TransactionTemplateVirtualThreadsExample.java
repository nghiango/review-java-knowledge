package lab.java25boot4.springtransactions.questions;

public class Q02TransactionTemplateVirtualThreadsExample {

    public static void main(String[] args) {
        // TransactionTemplate executes transactions programmatically within dedicated virtual
        // threads
        // keeping connection hold duration strictly minimal.
        boolean programmaticScopedTx = true;
        System.out.println(
                "Minimal connection hold duration: "
                        + programmaticScopedTx); // Minimal connection hold duration: true
    }
}
