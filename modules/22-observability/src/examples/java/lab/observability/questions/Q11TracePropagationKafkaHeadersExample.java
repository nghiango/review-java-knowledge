package lab.observability.questions;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Q11TracePropagationKafkaHeadersExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In event-driven messaging (Apache Kafka, RabbitMQ), trace context travels in record
        // headers.
        // Spring Kafka and Micrometer Tracing inject 'traceparent' as byte[] into ProducerRecord
        // headers.
        Map<String, byte[]> kafkaHeaders = new HashMap<>();

        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        kafkaHeaders.put("traceparent", traceparent.getBytes(StandardCharsets.UTF_8));

        String extractedHeader =
                new String(kafkaHeaders.get("traceparent"), StandardCharsets.UTF_8);
        boolean preservedAcrossBroker = extractedHeader.equals(traceparent); // true

        System.out.println("Trace context preserved in Kafka headers: " + preservedAcrossBroker);
    }
}
