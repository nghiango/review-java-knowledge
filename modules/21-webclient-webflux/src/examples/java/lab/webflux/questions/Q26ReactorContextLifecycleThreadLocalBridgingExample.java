package lab.webflux.questions;

import java.util.Map;

/**
 * Q26: How does Reactor Context propagate immutable key-value pairs upstream, and how is it bridged to MDC?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q26ReactorContextLifecycleThreadLocalBridgingExample {

    public static void main(String[] args) {
        // Reactor Context Mechanics:
        // 1. Immutable & Bound to Subscription:
        //    contextWrite(ctx -> ctx.put("traceId", "abc-123")) flows UPSTREAM from subscriber to publisher.
        //    Operators declared BEFORE contextWrite can read the context via deferContextual().
        //
        // 2. ThreadLocal Bridging via Micrometer Context Propagation:
        //    In Spring Boot 3+, Hooks.enableAutomaticContextPropagation() registers ThreadLocalAccessors
        //    (e.g., Slf4j MDC accessor), automatically copying Reactor Context keys into ThreadLocal
        //    during operator execution and clearing them afterwards.

        Map<String, String> contextFlow =
                Map.of(
                        "Direction", "Flows UPSTREAM from subscriber towards initial source publisher",
                        "Immutability", "Each put() returns a new Context instance; thread-safe",
                        "MDC Integration", "Bridged via Micrometer Context Propagation ThreadLocalAccessor");

        boolean contextFlowsUpstream =
                contextFlow.get("Direction").contains("UPSTREAM"); // true
    }
}
