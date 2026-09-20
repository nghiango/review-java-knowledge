package lab.springtransactions.questions;

public class Q19ThreadLocalTransactionStatePropagationExample {

    record TxContext(String txId, String boundConnection) {}

    private static final ThreadLocal<TxContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void main(String[] args) {
        // ThreadLocal holds transaction context per thread
        CONTEXT_HOLDER.set(new TxContext("tx-9012", "HikariProxyConnection@1234"));

        TxContext current = CONTEXT_HOLDER.get();
        boolean hasContext = current != null; // true
        String txId = current != null ? current.txId() : ""; // "tx-9012"

        // TaskDecorator or ScopedValue must explicitly copy context when offloading to worker
        // threads
        TxContext workerCopy = current;
        boolean propagated = workerCopy != null; // true

        CONTEXT_HOLDER.remove(); // Cleanup at boundary
        boolean isCleaned = CONTEXT_HOLDER.get() == null; // true

        System.out.println(
                "Has context: "
                        + hasContext
                        + ", txId: "
                        + txId
                        + ", propagated: "
                        + propagated
                        + ", cleaned: "
                        + isCleaned);
    }
}
