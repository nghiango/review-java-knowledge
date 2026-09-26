package lab.performance.questions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates JFR (Java Flight Recorder) continuous event streaming in production (available since JDK 14+),
 * capturing high-latency execution or allocation events asynchronously without disk dump stop-the-world overhead.
 */
public final class Q24JfrEventStreamingProductionProfilingExample {
    public static void main(String[] args) {
        int streamedEvents = 0;

        // In production: try (var rs = new RecordingStream()) { rs.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1)); ... }
        // Simulating event listener callback
        streamedEvents++;

        boolean isStreamingActive = streamedEvents > 0; // true
        System.out.println("JFR stream active: " + isStreamingActive);
    }
}
