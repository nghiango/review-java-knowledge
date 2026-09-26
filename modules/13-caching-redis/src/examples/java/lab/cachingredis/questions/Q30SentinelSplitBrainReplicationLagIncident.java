package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q30: Production Incident: Silent data loss during Redis Sentinel failover due to asynchronous replication lag and split-brain.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q30SentinelSplitBrainReplicationLagIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A network partition isolated Master A with a minority of clients and 1 Sentinel node.
        // The remaining Sentinels (majority quorum) detected Master A as unreachable and promoted
        // Replica B to be the new Master.
        // Old Master A continued accepting writes from the minority partition for 30 seconds.
        // When the network partition healed, Master A reconnected, was demoted to a replica of B,
        // and issued a full resync (PSYNC), completely wiping its own dataset and replacing it with B's!
        // All writes accepted by Master A during the split-brain were permanently and silently lost.

        boolean redisReplicationIsAsynchronousByDefault = true; // true

        // Prevention & Protection:
        // Configure safety boundaries in redis.conf:
        // min-replicas-to-write 1 (master rejects writes if fewer than 1 replica acknowledges within lag window)
        // min-replicas-max-lag 10 (maximum replication lag allowed in seconds)

        Map<String, String> safetyGuarantees =
                Map.of(
                        "Default Config", "Allows isolated master to accept writes; causes data loss on rejoin",
                        "min-replicas-to-write", "Halts writes on master if replicas disconnected; prevents split-brain loss");

        boolean preventsSilentDataLoss =
                safetyGuarantees.get("min-replicas-to-write").contains("prevents split-brain loss"); // true
    }
}
