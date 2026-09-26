package lab.webflux.questions;

import java.util.Map;

/**
 * Q29: Production Incident: Unbounded collectList() on real-time SSE/Kafka stream triggered JVM OutOfMemoryError.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29IncidentUnboundedBufferOomExample {

    public static void main(String[] args) {
        // Incident Scenario:
        // A developer exposed an endpoint to export market ticker updates:
        // Flux<TradeEvent> liveTrades = marketDataKafkaListener.getLiveStream();
        // Mono<List<TradeEvent>> allTrades = liveTrades.collectList();
        //
        // Failure Mechanism:
        // 1. collectList() aggregates stream items into an internal ArrayList until onComplete is emitted.
        // 2. Because a Kafka/SSE market feed is INFINITE, onComplete is NEVER emitted!
        // 3. The internal ArrayList grew indefinitely, absorbing millions of trade objects.
        // 4. Over 2 hours, the array exhausted 16GB of JVM heap, triggering non-stop Full GC pauses
        //    followed by fatal OutOfMemoryError (OOM) crashing the pod.

        boolean infiniteStreamCollectListCausesOom = true; // true

        // Remediation:
        // 1. Never call collectList(), toList(), or buffer() without an explicit limit or time window
        //    on continuous/infinite streams.
        // 2. Use bounded windowing: liveTrades.take(1000).collectList() or liveTrades.buffer(Duration.ofSeconds(10)).
        // 3. Stream elements directly as NDJSON or SSE chunks without buffering in memory:
        //    @GetMapping(value = "/trades", produces = MediaType.APPLICATION_NDJSON_VALUE)
        //    public Flux<TradeEvent> streamTrades() { return liveTrades; }

        Map<String, String> streamSafeguards =
                Map.of(
                        "collectList()", "Dangerous on infinite streams; awaits onComplete; leads to OOM",
                        "Direct NDJSON", "Streams elements chunk-by-chunk to client socket; zero memory accumulation");

        boolean streamingDirectlySavesMemory =
                streamSafeguards.get("Direct NDJSON").contains("zero memory accumulation"); // true
    }
}
