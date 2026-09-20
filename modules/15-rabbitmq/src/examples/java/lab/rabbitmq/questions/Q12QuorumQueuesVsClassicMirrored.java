package lab.rabbitmq.questions;

import java.util.Map;

/** Q12: Why are Quorum Queues (Raft consensus) replacing Classic Mirrored Queues (HA queues)? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q12QuorumQueuesVsClassicMirrored {

    public static void main(String[] args) {
        // Classic Mirrored Queues (ha-mode: all / exactly) - DEPRECATED since 3.8, removed in 4.0:
        // Suffered from synchronization blocking, message loss during split-brain / network
        // partitions,
        // and catastrophic failure during rolling cluster upgrades.
        boolean classicMirroredDeprecated = true; // true

        // Quorum Queues (x-queue-type: quorum):
        // Built on the Raft consensus algorithm.
        // Tolerates minority node failures without data loss or split-brain inconsistencies.
        // Requires a quorum (majority) of replicas to commit writes.
        Map<String, Object> quorumArgs = Map.of("x-queue-type", "quorum");
        boolean usesRaftConsensus = quorumArgs.get("x-queue-type").equals("quorum"); // true

        // Poison pill protection: Quorum queues provide x-delivery-count header tracking
        // redeliveries!
        boolean tracksDeliveryCountNatively = true; // true
    }
}
