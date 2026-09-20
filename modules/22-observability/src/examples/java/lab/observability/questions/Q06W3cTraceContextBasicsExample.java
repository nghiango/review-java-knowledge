package lab.observability.questions;

public class Q06W3cTraceContextBasicsExample {

    record TraceContext(String traceId, String spanId, String parentSpanId, boolean sampled) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // W3C TraceContext standard defines:
        // - traceparent header: 00-{trace_id}-{span_id}-{trace_flags}
        // - traceId (16 bytes hex, 32 chars): Globally unique ID for the entire end-to-end request
        // journey.
        // - spanId (8 bytes hex, 16 chars): ID for a specific operation or segment within a
        // service.
        TraceContext rootSpan =
                new TraceContext(
                        "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", null, true);

        String traceparentHeader = "00-" + rootSpan.traceId() + "-" + rootSpan.spanId() + "-01";

        boolean headerMatchesW3c = traceparentHeader.startsWith("00-"); // true
        int traceIdLength = rootSpan.traceId().length(); // 32

        System.out.println("Formatted W3C traceparent header: " + traceparentHeader);
        System.out.println("Trace ID hex length: " + traceIdLength);
    }
}
