package lab.kafka.questions;

import java.util.List;

/**
 * Q05: How do consumer groups enable horizontal scaling, and how do partition assignors divide
 * work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q05ConsumerGroupsAndRebalancing {

    public static void main(String[] args) {
        int topicPartitions = 8;
        int activeConsumers = 4;

        // With 8 partitions and 4 consumers, each consumer receives exactly 2 partitions
        int partitionsPerConsumer = topicPartitions / activeConsumers; // 2

        // If consumer count exceeds partition count (e.g. 10 consumers for 8 partitions),
        // the excess consumers stay idle with 0 partitions assigned
        int excessConsumers = 10;
        int idleConsumers = excessConsumers - topicPartitions; // 2

        // RangeAssignor assigns contiguous partition ranges per topic
        // RoundRobinAssignor distributes partitions across consumers one-by-one
        // CooperativeStickyAssignor preserves existing assignments and only migrates unassigned
        // partitions
        List<String> assignorTypes = List.of("Range", "RoundRobin", "CooperativeSticky");
        boolean supportsZeroDowntimeRebalance = assignorTypes.contains("CooperativeSticky"); // true
    }
}
