package lab.rabbitmq.questions;

import java.util.List;

/** Q13: How do RabbitMQ Memory and Disk alarms enforce backpressure on publishing clients? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q13RabbitMqFlowControlAlarms {

    public static void main(String[] args) {
        // Memory Alarm: Triggered when broker RAM usage exceeds vm_memory_high_watermark (default
        // 40% of RAM)
        // Disk Alarm: Triggered when free disk space falls below disk_free_limit (default 50MB or
        // 1.5x RAM)
        List<String> alarms = List.of("vm_memory_high_watermark", "disk_free_limit");
        boolean protectsBrokerFromCrash = (alarms.size() == 2); // true

        // Alarm Impact:
        // When an alarm trips, RabbitMQ BLOCKS all publishing connections!
        // It stops reading bytes from publisher TCP sockets (TCP zero-window), causing producer
        // threads
        // to block on socket write buffers without crashing the broker.
        boolean blocksPublisherTcpSockets = true; // true

        // Crucial: Consumers continue consuming and acknowledging!
        // This drains queues and frees broker memory to clear the alarm.
        boolean consumersRemainActiveToDrain = true; // true
    }
}
