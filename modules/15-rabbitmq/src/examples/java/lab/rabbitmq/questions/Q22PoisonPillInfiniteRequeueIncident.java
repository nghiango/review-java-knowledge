package lab.rabbitmq.questions;

import java.util.List;

/**
 * Q22: Production incident post-mortem: How an unhandled NullPointerException caused 100% CPU
 * lockup and queue stall due to infinite requeue.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q22PoisonPillInfiniteRequeueIncident {

    public static void main(String[] args) {
        // Incident chain of events:
        // 1. Upstream producer deployed a payload with a null tax ID
        // 2. Downstream consumer listener threw an unhandled NullPointerException
        // 3. Container error handler caught the exception and invoked basicNack with requeue = true
        // 4. RabbitMQ placed the message immediately back at the front of the queue
        // 5. Consumer re-received the message 0.1ms later, failed again, and re-nacked
        // 6. Loop executed 10,000 times/sec, saturating CPU at 100%, filling disk with logs,
        // stalling 50,000 valid orders
        List<String> rootCauses =
                List.of(
                        "basicNack with requeue=true on non-transient error",
                        "Missing Dead Letter Exchange (DLX) routing",
                        "Absence of retry counter / delivery-limit");

        // Remediation:
        // 1. Configured Quorum queue with delivery-limit = 3 (auto-drops/routes to DLX on 4th
        // attempt)
        // 2. Reject fatal exceptions with requeue = false immediately
        // 3. Configure Spring AMQP RepublishMessageRecoverer to forward poison messages to DLQ
        boolean poisonPillSafeguardActive = true; // true
    }
}
