package lab.observability.questions;

import java.util.HashMap;
import java.util.Map;

public class Q10TracePropagationHttpHeaderExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Distributed tracing propagates context over HTTP using standard headers:
        // - W3C TraceContext: 'traceparent' header (00-traceId-spanId-flags)
        // - B3 (Zipkin/Brave): 'X-B3-TraceId', 'X-B3-SpanId', 'X-B3-Sampled'
        Map<String, String> httpHeaders = new HashMap<>();

        // Injecting W3C traceparent header
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String spanId = "00f067aa0ba902b7";
        httpHeaders.put("traceparent", "00-" + traceId + "-" + spanId + "-01");

        boolean hasW3cHeader = httpHeaders.containsKey("traceparent"); // true
        boolean isValidHeaderFormat = httpHeaders.get("traceparent").startsWith("00-"); // true

        System.out.println("Trace header injected: " + hasW3cHeader);
        System.out.println("W3C traceparent value: " + httpHeaders.get("traceparent"));
    }
}
