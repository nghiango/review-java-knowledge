package lab.observability.questions;

import java.util.List;

/**
 * Demonstrates OpenTelemetry Collector pipeline configuration model:
 * Receivers ingest telemetry, Processors transform/batch/filter, and Exporters deliver to backends.
 */
public class Q25OtelCollectorPipelinesConfigExample {

    record CollectorPipeline(String signalType, List<String> receivers, List<String> processors, List<String> exporters) {}

    public static void main(String[] args) {
        // In an OTel Collector configuration (otel-collector-config.yaml):
        // pipelines:
        //   traces:
        //     receivers: [otlp]
        //     processors: [memory_limiter, batch]
        //     exporters: [otlp/tempo, prometheus]
        CollectorPipeline tracesPipeline = new CollectorPipeline(
                "traces",
                List.of("otlp"),
                List.of("memory_limiter", "batch"),
                List.of("otlp/tempo")
        );

        boolean hasBatchProcessor = tracesPipeline.processors().contains("batch"); // true
        boolean hasMemoryLimiter = tracesPipeline.processors().contains("memory_limiter"); // true

        System.out.println("Pipeline configured with batching: " + hasBatchProcessor);
        System.out.println("Pipeline configured with memory protection: " + hasMemoryLimiter);
    }
}
