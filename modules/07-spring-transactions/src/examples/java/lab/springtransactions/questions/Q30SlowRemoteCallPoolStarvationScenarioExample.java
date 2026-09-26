package lab.springtransactions.questions;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class Q30SlowRemoteCallPoolStarvationScenarioExample {
    private Q30SlowRemoteCallPoolStarvationScenarioExample() {}

    // Simulated Hikari pool with 10 connections
    public static class ConnectionPool {
        private final AtomicInteger availableConnections = new AtomicInteger(10);

        public boolean borrowConnection() {
            int current;
            do {
                current = availableConnections.get();
                if (current <= 0) return false;
            } while (!availableConnections.compareAndSet(current, current - 1));
            return true;
        }

        public void releaseConnection() {
            availableConnections.incrementAndGet();
        }

        public int getAvailable() {
            return availableConnections.get();
        }
    }

    // Problematic pattern: remote call executed inside database transaction boundary
    public static void executeFlawedPattern(ConnectionPool pool) {
        boolean borrowed = pool.borrowConnection(); // Connection acquired!
        try {
            // UNTIMED HTTP CALL: Thread sits idle waiting on external payment gateway response for
            // 5 seconds.
            // The JDBC connection is completely blocked and unusable by any other request!
            simulateExternalCall();
        } finally {
            pool.releaseConnection();
        }
    }

    private static void simulateExternalCall() {
        // Blocks thread while holding connection
    }

    public static void main(String[] args) {
        ConnectionPool pool = new ConnectionPool();
        boolean success = pool.borrowConnection();
        int remaining = pool.getAvailable(); // 9 (1 connection held hostage during remote call!)
        pool.releaseConnection();
    }
}
