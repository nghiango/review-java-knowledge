package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q30: Production Incident: Dual master split-brain and divergent queue history under network partition.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q30SplitBrainNetworkPartitionIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A 3-node RabbitMQ cluster was configured with legacy Classic Mirrored Queues (ha-mode: all)
        // and default partition handling mode: cluster_partition_handling = ignore.
        // A transient top-of-rack network switch failure severed Node 1 from Nodes 2 and 3.

        // Failure Mechanism:
        // 1. With 'ignore' mode, Node 1 assumed Nodes 2 and 3 were dead.
        // 2. Nodes 2 and 3 assumed Node 1 was dead.
        // 3. Both sides promoted independent queue masters for every mirrored queue.
        // 4. Clients partitioned across the two network halves published different messages with duplicate
        //    delivery tags to the "same" queue.
        // 5. When the network switch recovered, RabbitMQ detected divergent, conflicting queue states.
        //    One side's messages were completely overwritten and permanently discarded!

        boolean ignoreModePermitsSplitBrainDataLoss = true; // true

        // Remediation:
        // 1. Never use 'ignore' in production. Configure: cluster_partition_handling = pause_minority.
        //    Under pause_minority, isolated Node 1 detects it is in the minority (1 of 3) and automatically
        //    pauses itself, rejecting writes and preventing dual-master split-brain.
        // 2. Migrate from Classic Mirrored Queues to Quorum Queues, which rely on Raft majority consensus
        //    and are mathematically immune to split-brain data corruption.

        Map<String, String> partitionModes =
                Map.of(
                        "ignore", "Allows dual-master divergence; causes silent data loss on rejoin",
                        "pause_minority", "Minority pauses immediately; prevents split-brain writes",
                        "quorum_queues", "Raft consensus protocol; strictly guarantees safety across network partitions");

        boolean quorumQueuesImmuneToSplitBrain =
                partitionModes.get("quorum_queues").contains("guarantees safety"); // true
    }
}
