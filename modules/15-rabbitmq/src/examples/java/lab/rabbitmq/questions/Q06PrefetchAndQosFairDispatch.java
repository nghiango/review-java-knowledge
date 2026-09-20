package lab.rabbitmq.questions;

/** Q06: How does basic.qos prefetch count enforce fair dispatch across competing consumers? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q06PrefetchAndQosFairDispatch {

    public static void main(String[] args) {
        // Without prefetch (or prefetch = 0):
        // RabbitMQ uses round-robin push regardless of worker load.
        // If Worker 1 receives 10 heavy tasks and Worker 2 receives 10 light tasks,
        // Worker 2 finishes in seconds while Worker 1 is overwhelmed, causing worker starvation.
        boolean unthrottledPushCausesSkew = true; // true

        // basic.qos(prefetchCount = 1):
        // RabbitMQ will not deliver a new message to a worker until that worker has acknowledged
        // the previous message, implementing optimal dynamic load balancing (fair dispatch).
        int fairDispatchPrefetch = 1;
        boolean fairDispatchActive = (fairDispatchPrefetch == 1); // true

        // For high-throughput lightweight tasks, setting prefetch = 20-100 saturates network
        // pipeline
        int highThroughputPrefetch = 50;
    }
}
