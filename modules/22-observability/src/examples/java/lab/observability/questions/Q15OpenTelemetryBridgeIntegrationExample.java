package lab.observability.questions;

public class Q15OpenTelemetryBridgeIntegrationExample {

    record TracingBridgeConfig(
            String tracerBridge, String contextStandard, String exporterProtocol) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In Spring Boot 3, Micrometer Tracing abstracts distributed tracing implementations:
        // - micrometer-tracing-bridge-otel: Uses OpenTelemetry SDK for span lifecycles.
        // - micrometer-tracing-bridge-brave: Uses Zipkin Brave for span lifecycles.
        // - W3C TraceContext is default propagation standard.
        // - Spans export to OpenTelemetry Collector via OTLP (OpenTelemetry Protocol over
        // gRPC/HTTP).
        TracingBridgeConfig config =
                new TracingBridgeConfig(
                        "micrometer-tracing-bridge-otel",
                        "W3C TraceContext (traceparent)",
                        "OTLP (gRPC / protobuf)");

        boolean isOtelBridge = config.tracerBridge().contains("otel"); // true
        boolean isW3cStandard = config.contextStandard().contains("W3C"); // true

        System.out.println("Configured OTel bridge: " + isOtelBridge);
        System.out.println("Configured W3C context: " + isW3cStandard);
    }
}
