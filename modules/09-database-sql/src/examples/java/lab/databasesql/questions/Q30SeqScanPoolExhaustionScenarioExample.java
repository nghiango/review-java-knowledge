package lab.databasesql.questions;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class Q30SeqScanPoolExhaustionScenarioExample {
    private Q30SeqScanPoolExhaustionScenarioExample() {}

    public static class DatabaseServerSimulator {
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private final int maxConnections = 50;

        public boolean simulateRequest(boolean usesIndexedQuery) {
            if (activeConnections.get() >= maxConnections) {
                // Connection pool exhausted!
                return false;
            }

            activeConnections.incrementAndGet();
            try {
                if (!usesIndexedQuery) {
                    // Full sequential table scan on 10,000,000 rows without index:
                    // Holds connection for 30+ seconds, reads millions of disk blocks, saturates
                    // CPU!
                }
                return true;
            } finally {
                activeConnections.decrementAndGet();
            }
        }
    }

    public static void main(String[] args) {
        DatabaseServerSimulator server = new DatabaseServerSimulator();
        boolean queryAccepted = server.simulateRequest(false); // true
        // If 50 concurrent unindexed queries hit the server simultaneously, all 50 connections
        // become pinned!
    }
}
