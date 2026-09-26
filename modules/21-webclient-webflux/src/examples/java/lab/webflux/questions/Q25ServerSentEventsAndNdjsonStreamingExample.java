package lab.webflux.questions;

import java.util.Map;

/**
 * Q25: How does Spring WebFlux implement infinite real-time streaming with SSE and NDJSON?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q25ServerSentEventsAndNdjsonStreamingExample {

    public static void main(String[] args) {
        // Real-Time Streaming Media Types:
        // 1. MediaType.TEXT_EVENT_STREAM_VALUE (Server-Sent Events / SSE):
        //    Standard W3C event-stream format with 'data:', 'event:', and 'id:' fields.
        //    Native browser support via JavaScript EventSource. Unidirectional server-to-client push.
        //
        // 2. MediaType.APPLICATION_NDJSON_VALUE (Newline Delimited JSON):
        //    Emits separate JSON objects separated by '\n'.
        //    Ideal for machine-to-machine streaming, large bulk exports, and microservice pipelines.

        Map<String, String> streamingFormats =
                Map.of(
                        "text/event-stream", "Browser native SSE; handles automatic reconnection and event IDs",
                        "application/x-ndjson", "Newline-delimited JSON; machine-to-machine streaming; low framing overhead");

        boolean sseSupportsBrowserEventSource =
                streamingFormats.get("text/event-stream").contains("EventSource"); // true
    }
}
