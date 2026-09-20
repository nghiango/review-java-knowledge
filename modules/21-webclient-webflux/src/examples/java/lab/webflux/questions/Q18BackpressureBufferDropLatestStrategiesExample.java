package lab.webflux.questions;

import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;

public class Q18BackpressureBufferDropLatestStrategiesExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // When upstream produces faster than downstream can consume, Reactor provides backpressure
        // overflow strategies:
        // - onBackpressureBuffer(maxSize): Queues items in memory (risks OutOfMemoryError if
        // unbounded).
        // - onBackpressureDrop(): Drops excess items that cannot be requested.
        // - onBackpressureLatest(): Keeps only the most recent element, discarding intermediate
        // unconsumed values.
        List<Integer> collected = new ArrayList<>();

        Flux.range(1, 10).onBackpressureLatest().subscribe(collected::add);

        int totalCollected = collected.size(); // 10
        boolean strategyConfigured = totalCollected > 0; // true

        System.out.println("Backpressure stream handled elements: " + totalCollected);
    }
}
