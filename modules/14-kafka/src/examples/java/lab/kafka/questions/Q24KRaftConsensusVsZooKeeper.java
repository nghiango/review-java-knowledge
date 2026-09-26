package lab.kafka.questions;

import java.util.Map;

/**
 * Q24: How does KRaft (Kafka Raft metadata mode) eliminate ZooKeeper and accelerate partition failover?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q24KRaftConsensusVsZooKeeper {

    public static void main(String[] args) {
        // ZooKeeper vs KRaft (KIP-500):
        // In ZooKeeper mode:
        // - Metadata is stored externally in ZooKeeper ZNodes.
        // - One broker is active controller; on failover, new controller must load all metadata from ZK.
        // - Limited cluster scalability to ~200,000 partitions due to ZK watch latency and state sync.

        // In KRaft Mode (Kafka 3.3+ production ready, Kafka 4.0+ mandatory):
        // - Quorum of Controller nodes runs an event-driven Raft consensus algorithm.
        // - Metadata is stored in a replicated internal Kafka topic: @metadata.
        // - Controller nodes maintain in-memory metadata caches kept continuously in-sync via Raft log.
        // - Scalability increases to millions of partitions with near-instant controller failover.

        Map<String, String> architectureComparison =
                Map.of(
                        "ZooKeeper", "External quorum; high failover time loading ZNodes; ~200k partition limit",
                        "KRaft", "Internal Raft metadata log; sub-second failover; millions of partitions supported");

        boolean kraftEliminatesZooKeeperDependency =
                architectureComparison.get("KRaft").contains("sub-second failover"); // true
    }
}
