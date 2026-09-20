package lab.distributeddata.questions;

import java.util.Map;

public class Q18DistributedTracingTraceIdPropagationInEventsExample {

    record TracingHeaders(String traceId, String spanId) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // W3C Trace Context (traceparent) must be propagated into Kafka RecordHeaders
        // to stitch together asynchronous multi-service distributed traces in OpenTelemetry/Jaeger.
        Map<String, String> headers =
                Map.of("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        boolean hasW3cHeader = headers.containsKey("traceparent"); // true
        System.out.println("Trace context preserved in messaging metadata: " + hasW3cHeader);
    }
}
