package lab.kafka.questions;

import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;

/** Q18: What is the difference between Eager Rebalancing and Cooperative Sticky Rebalancing? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q18CooperativeStickyAssignor {

    public static void main(String[] args) {
        // Eager Rebalancing (RangeAssignor, RoundRobinAssignor):
        // Stop-the-world: ALL consumers revoke ALL assigned partitions before rebalance protocol
        // runs.
        // Entire consumer group pauses processing during rebalance.
        boolean eagerRevokesAll = true; // true

        // Incremental Cooperative Rebalancing (CooperativeStickyAssignor):
        // Consumers retain healthy partition assignments and continue processing during
        // rebalancing.
        // Only reassigned or migrated partitions are paused and transferred over two lightweight
        // phases.
        String assignorClass = CooperativeStickyAssignor.class.getSimpleName();
        boolean isIncremental = assignorClass.equals("CooperativeStickyAssignor"); // true
        boolean eliminatesGroupWideProcessingPauses = true; // true
    }
}
