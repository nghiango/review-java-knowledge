package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q28SafepointPollingHandshakeExample {
    private Q28SafepointPollingHandshakeExample() {}

    public static void main(String[] args) {
        // Modern JVMs (Java 10+) use Thread-Local Handshakes to execute operations
        // on individual threads without forcing a global Stop-The-World (STW) safepoint.
        //
        // Operations using Thread-Local Handshakes:
        // 1. Biased locking revocation on a single thread
        // 2. Thread stack traces / thread dumps of an individual thread
        // 3. JIT deoptimization of a single thread's execution frame
        //
        // Global Safepoints are still required for:
        // 1. Stop-the-world GC phases (initial mark, remark)
        // 2. Class redefinition / unloading
        // 3. CodeCache cleanup and sweeping
        //
        // Compiler inserts safepoint poll checks on method entry, exit, and loop back-edges.
        // HotSpot VM flag: -XX:+PrintSafepointStatistics or JFR event `jdk.SafepointBegin`
        long activeThreads = Thread.activeCount(); // > 0
    }
}
