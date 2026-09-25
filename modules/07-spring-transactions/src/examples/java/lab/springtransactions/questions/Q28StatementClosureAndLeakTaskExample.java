package lab.springtransactions.questions;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public final class Q28StatementClosureAndLeakTaskExample {
    private Q28StatementClosureAndLeakTaskExample() {}

    // Simulated HikariCP ProxyConnection and LeakTask:
    // When a connection is borrowed from the pool, if leakDetectionThreshold > 0 (e.g. 10000ms),
    // HikariCP schedules a ProxyLeakTask on a HouseKeeper executor thread.
    // The task captures the caller's stack trace at the point of borrow.
    public static class LeakTaskSimulator {
        private final long leakThresholdMs;
        private final StackTraceElement[] allocationStackTrace;
        private final AtomicBoolean returned = new AtomicBoolean(false);

        public LeakTaskSimulator(long leakThresholdMs) {
            this.leakThresholdMs = leakThresholdMs;
            this.allocationStackTrace = Thread.currentThread().getStackTrace();
        }

        public void returnConnection() {
            returned.set(true);
        }

        public String evaluateLeak(long heldDurationMs) {
            if (!returned.get() && heldDurationMs > leakThresholdMs) {
                return "Apparent connection leak detected for connection held longer than "
                    + leakThresholdMs + "ms! Allocation stack trace captured.";
            }
            return "OK";
        }
    }

    public static void main(String[] args) {
        LeakTaskSimulator leakDetector = new LeakTaskSimulator(5000L);
        // Connection held for 7000ms without close():
        String warning = leakDetector.evaluateLeak(7000L);
        boolean isLeaking = warning.contains("Apparent connection leak detected"); // true

        leakDetector.returnConnection();
    }
}
