package lab.webflux.questions;

import java.time.Duration;

public class Q09ReactorNettyConnectionPoolAcquireExample {

    record ConnectionPoolMetrics(
            int maxConnections, int pendingAcquires, Duration acquireTimeout) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Reactor Netty allocates an outbound connection pool per remote host (default max 500
        // connections).
        // If all 500 connections are active and incoming requests exceed maxPendingAcquires,
        // or if waiting in the pending queue exceeds pendingAcquireTimeout (default 45s),
        // Reactor Netty throws PoolAcquireTimeoutException.
        ConnectionPoolMetrics pool = new ConnectionPoolMetrics(500, 1000, Duration.ofSeconds(45));

        int maxChannels = pool.maxConnections(); // 500
        boolean isSaturatedWhenExceeded = pool.pendingAcquires() > pool.maxConnections(); // true

        System.out.println("Max connections per remote host: " + maxChannels);
        System.out.println("Pending queue saturated: " + isSaturatedWhenExceeded);
    }
}
