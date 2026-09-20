package lab.kafka.questions;

import java.util.List;

/**
 * Q02: What is the role of the In-Sync Replicas (ISR) list, and how does min.insync.replicas
 * prevent data loss?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q02ReplicationAndIsrMechanics {

    public static void main(String[] args) {
        // ISR contains replicas that are fully caught up with the partition leader
        List<Integer> isrBrokers = List.of(101, 102, 103);
        int replicationFactor = 3;
        int minInsyncReplicas = 2;

        // If one follower broker falls behind or crashes, ISR shrinks to 2
        int remainingIsr = 2;
        boolean canAcknowledgeWrites = remainingIsr >= minInsyncReplicas; // true

        // If another follower crashes and ISR drops to 1, writes with acks=all throw
        // NotEnoughReplicasException
        int degradedIsr = 1;
        boolean rejectsWritesToPreventDataLoss = degradedIsr < minInsyncReplicas; // true
    }
}
