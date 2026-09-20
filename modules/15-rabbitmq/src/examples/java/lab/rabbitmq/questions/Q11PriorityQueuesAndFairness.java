package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q11: How do priority queues work in RabbitMQ, and what are the trade-offs regarding consumer
 * starvation?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q11PriorityQueuesAndFairness {

    public static void main(String[] args) {
        // Priority Queue declaration: x-max-priority = 10 (recommended range: 1 to 10)
        // Higher priority values consume additional broker memory (Erlang process sub-queues)
        int maxPriority = 10;
        Map<String, Object> queueArgs = Map.of("x-max-priority", maxPriority);

        // Publishers assign integer priority in basic properties:
        // builder.priority(9) for urgent VIP payments, priority(1) for standard reports
        int vipPriority = 9;
        int standardPriority = 1;
        boolean vipDeliveredAheadOfStandard = (vipPriority > standardPriority); // true

        // Starvation trade-off:
        // If a steady stream of high-priority messages arrives, low-priority messages can remain
        // stuck in the queue indefinitely (priority inversion / starvation).
        boolean risksStarvationUnderHeavyHighPriorityLoad = true; // true
    }
}
