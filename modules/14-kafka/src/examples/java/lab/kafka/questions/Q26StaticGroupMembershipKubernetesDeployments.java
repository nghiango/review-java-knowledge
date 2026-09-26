package lab.kafka.questions;

import java.util.Map;

/**
 * Q26: How does Static Group Membership (group.instance.id) prevent rebalance storms during Kubernetes rolling restarts?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q26StaticGroupMembershipKubernetesDeployments {

    public static void main(String[] args) {
        // Dynamic Membership (Default):
        // Each consumer receives a randomly generated member ID on startup (e.g., consumer-1-<UUID>).
        // During a Kubernetes rolling restart of 20 pods:
        // - Pod termination sends LeaveGroupRequest or times out session.
        // - Group coordinator triggers a full rebalance.
        // - 20 rolling restarts trigger 20 consecutive rebalances, stalling consumption for minutes.

        // Static Membership (KIP-345):
        // Assign a persistent identifier: group.instance.id (e.g. mapped from StatefulSet pod name: "orders-worker-1").
        // On pod restart:
        // - Consumer does not send LeaveGroupRequest on shutdown.
        // - Coordinator preserves partition assignments for the static instance up to session.timeout.ms.
        // - When the new container starts with the same group.instance.id, it resumes consumption of its
        //   assigned partitions immediately WITHOUT triggering a group rebalance!

        Map<String, String> membershipModes =
                Map.of(
                        "Dynamic", "Ephemeral UUID; rebalance triggered on every pod termination/startup",
                        "Static", "Fixed group.instance.id; zero rebalances during rolling restarts within session timeout");

        boolean avoidsRebalancesOnRollingRestart =
                membershipModes.get("Static").contains("zero rebalances"); // true
    }
}
