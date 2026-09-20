package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q14: What are the fundamental pitfalls of Redis distributed locks (GC pauses, clock drift,
 * Redlock controversy, fencing tokens)?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q14DistributedLockPitfalls {

    public static void main(String[] args) {
        // Pitfalls detailed by Martin Kleppmann and distributed systems research:
        Map<String, String> pitfalls =
                Map.of(
                        "Stop-the-World GC Pause",
                        "Thread A holds lock for 5s. JVM pauses for 7s. Lock expires. Thread B acquires lock. Thread A resumes and executes concurrently.",
                        "Asymmetric Clock Drift",
                        "System clock jumps forward on a Redis node due to NTP, causing premature key expiration.",
                        "Async Replication Loss",
                        "Client acquires lock on Master. Master crashes before replicating to Slave. Slave promoted to Master. New client acquires SAME lock!");

        // Solution: Fencing Tokens (Monotonically increasing token passed to storage).
        // The storage layer rejects any write with a token lower than the highest seen token.
        boolean fencingTokenSolvesGcPauseOverlap = true; // true

        // Redlock Algorithm:
        // Acquires locks on N/2 + 1 independent Redis master nodes.
        // Controversial: Relies on synchronized hardware wall clocks; still susceptible to process
        // pauses without fencing tokens.
        boolean redlockControversialForSafetyCriticalData = true; // true
    }
}
