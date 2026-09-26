package lab.rabbitmq.questions;

import java.util.Map;

/**
 * Q27: How does RabbitMQ flow control trigger publisher blocking when memory or disk alarms fire?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q27MemoryAndDiskAlarmFlowControl {

    public static void main(String[] args) {
        // Alarms in RabbitMQ:
        // 1. Memory Alarm (vm_memory_high_watermark):
        //    Default is 0.4 (40% of host RAM). When Erlang process memory exceeds this threshold,
        //    the memory alarm fires cluster-wide.
        // 2. Disk Alarm (disk_free_limit):
        //    Default is 50MB (often raised to 5-10GB in production). When free disk space falls below limit.

        // Flow Control Behavior:
        // - RabbitMQ immediately STOPS reading from all incoming publisher TCP sockets.
        // - Sends a connection.blocked frame to connected AMQP clients.
        // - Producers calling basicPublish() block or hang waiting for TCP socket window availability.
        // - Crucially: CONSUMERS ARE NOT BLOCKED! Consumers can continue reading and acknowledging
        //   messages to drain the queues and lower RAM usage until the alarm clears.

        double memoryThreshold = 0.40; // 40% RAM
        boolean memoryAlarmBlocksPublishersOnly = true; // true
        boolean consumersCanStillDrainQueues = true; // true

        Map<String, String> flowControlStatus =
                Map.of(
                        "Publishers", "TCP sockets throttled; connection.blocked notification sent",
                        "Consumers", "Unblocked; allowed to consume and acknowledge messages to clear alarm");

        boolean publishersThrottled =
                flowControlStatus.get("Publishers").contains("connection.blocked"); // true
    }
}
