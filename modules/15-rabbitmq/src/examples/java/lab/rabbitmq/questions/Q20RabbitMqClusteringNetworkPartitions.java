package lab.rabbitmq.questions;

import java.util.List;

/** Q20: How does RabbitMQ handle network partitions, and what are the partition handling modes? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q20RabbitMqClusteringNetworkPartitions {

    public static void main(String[] args) {
        // RabbitMQ clusters rely on low-latency, reliable LAN connections.
        // When a network split occurs, nodes in different partitions cannot coordinate:
        List<String> partitionModes = List.of("ignore", "pause_minority", "autoheal");

        // ignore: Does nothing. Both sides continue accepting writes, resulting in divergence and
        // split-brain!
        boolean ignoreCausesSplitBrain = true; // true

        // pause_minority: Nodes in a minority partition pause themselves automatically, rejecting
        // client connections
        // until the network partition heals. The majority partition continues operating safely.
        // (Recommended)
        boolean pauseMinorityPreventsSplitBrain = true; // true

        // With Quorum Queues, Raft handles partition splits natively without pausing the entire
        // broker!
        boolean quorumQueuesHandleNatively = true; // true
    }
}
