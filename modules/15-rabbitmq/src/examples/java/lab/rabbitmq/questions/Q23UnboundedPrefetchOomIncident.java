package lab.rabbitmq.questions;

import java.util.List;

/**
 * Q23: Production incident post-mortem: How an unbounded prefetch configuration caused an
 * OutOfMemoryError crash loop across worker pods during a flash sale.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q23UnboundedPrefetchOomIncident {

    public static void main(String[] args) {
        // Incident chain of events:
        // 1. Flash sale generated 20,000 PDF invoice generation requests
        // 2. Worker pods had prefetchCount left at 0 (unbounded in AMQP)
        // 3. The first worker pod to boot received 8,000 tasks pushed simultaneously by RabbitMQ
        // 4. Each PDF task consumed 2MB of memory; heap usage surged to 16GB, triggering OOM killed
        // by Kubernetes
        // 5. When Pod 1 died, RabbitMQ requeued all 8,000 tasks and pushed them to Pod 2,
        // immediately killing Pod 2
        // 6. Cascading domino crash loop across all 10 worker pods in the cluster
        List<String> incidentRootCauses =
                List.of(
                        "Prefetch count set to 0 (unbounded) on heavy tasks",
                        "Push-based distribution overwhelming slow consumers",
                        "Domino requeueing upon worker termination");

        // Remediation:
        // 1. Set prefetchCount = 5 on SimpleRabbitListenerContainerFactory
        // 2. Use Quorum Queues with dead-lettering to prevent thundering herds on pod restarts
        // 3. Add Prometheus alert on rabbitmq_queue_messages_unacked / consumer_count ratio
        boolean cascadingOomPrevented = true; // true
    }
}
