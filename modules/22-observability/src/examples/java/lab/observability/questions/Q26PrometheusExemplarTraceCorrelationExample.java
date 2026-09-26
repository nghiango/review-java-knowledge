package lab.observability.questions;

import java.util.Map;

/**
 * Demonstrates OpenMetrics Exemplars linking Prometheus metrics to distributed trace spans,
 * enabling 1-click drill-down from a metric latency spike to the exact offending trace ID in Grafana.
 */
public class Q26PrometheusExemplarTraceCorrelationExample {

    record OpenMetricsSampleWithExemplar(
            String metricName,
            double value,
            String traceId,
            String spanId
    ) {
        public String toOpenMetricsFormat() {
            // OpenMetrics format: metric_name 0.456 # {trace_id="...",span_id="..."} timestamp
            return String.format("%s %f # {trace_id=\"%s\",span_id=\"%s\"} %d",
                    metricName, value, traceId, spanId, System.currentTimeMillis());
        }
    }

    public static void main(String[] args) {
        String activeTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String activeSpanId = "00f067aa0ba902b7";

        OpenMetricsSampleWithExemplar sample = new OpenMetricsSampleWithExemplar(
                "http_server_requests_seconds_bucket{le=\"0.5\"}",
                1.0,
                activeTraceId,
                activeSpanId
        );

        String output = sample.toOpenMetricsFormat();
        boolean hasExemplarTrace = output.contains("trace_id=\"" + activeTraceId + "\""); // true
        System.out.println("Rendered OpenMetrics exemplar: " + output);
        System.out.println("Contains exemplar trace ID: " + hasExemplarTrace);
    }
}
