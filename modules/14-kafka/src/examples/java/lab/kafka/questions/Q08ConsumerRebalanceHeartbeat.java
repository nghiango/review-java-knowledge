package lab.kafka.questions;

/**
 * Q08: How do session.timeout.ms, heartbeat.interval.ms, and max.poll.interval.ms govern consumer
 * health?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q08ConsumerRebalanceHeartbeat {

    public static void main(String[] args) {
        // Heartbeat thread runs in background sending periodic heartbeats to Group Coordinator
        // broker
        int heartbeatIntervalMs = 3000; // 3 seconds
        int sessionTimeoutMs = 45000; // 45 seconds (must be at least 3x heartbeat interval)
        boolean validHeartbeatRatio = (sessionTimeoutMs >= 3 * heartbeatIntervalMs); // true

        // max.poll.interval.ms: Maximum allowed delay between successive poll() invocations by
        // foreground thread
        int maxPollIntervalMs = 300000; // 5 minutes default

        // If processing a single batch exceeds max.poll.interval.ms, coordinator considers consumer
        // dead
        // and triggers group rebalance, even if heartbeat thread is still alive and sending
        // heartbeats
        int heavyBatchDurationMs = 360000; // 6 minutes
        boolean triggersRebalanceStorm = heavyBatchDurationMs > maxPollIntervalMs; // true
    }
}
