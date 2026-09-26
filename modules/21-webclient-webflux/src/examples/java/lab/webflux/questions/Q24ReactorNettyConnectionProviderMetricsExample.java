package lab.webflux.questions;

import java.util.Map;

/**
 * Q24: How do you configure and monitor Reactor Netty ConnectionProvider to prevent leaks and timeouts?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q24ReactorNettyConnectionProviderMetricsExample {

    public static void main(String[] args) {
        // Reactor Netty ConnectionProvider tuning:
        // - maxConnections: Maximum number of active and acquired TCP sockets (default: 500)
        // - pendingAcquireMaxCount: Maximum requests queued waiting for a free socket (default: 2 * maxConnections = 1000)
        // - pendingAcquireTimeout: Max wait time to acquire a connection before throwing PoolAcquireTimeoutException (default: 45s)
        // - maxIdleTime: Sockets idle longer than this duration are closed (e.g., 20s to prevent stale sockets after AWS ELB drops)

        int maxConnections = 50;
        int pendingAcquireMaxCount = 200;
        long pendingAcquireTimeoutMs = 2000; // Fast-fail in 2s instead of hanging for 45s

        Map<String, String> poolConfig =
                Map.of(
                        "maxConnections", "50 (limits socket saturation)",
                        "pendingAcquireTimeout", "2000ms (fails fast; prevents thread queue build-up)",
                        "maxIdleTime", "20s (avoids silent RST drops by upstream load balancers)");

        boolean preventsStaleConnectionExceptions =
                poolConfig.containsKey("maxIdleTime"); // true
    }
}
