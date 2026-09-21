package lab.java25boot4.corejava;

import java.util.List;
import java.util.stream.Gatherers;

/**
 * Demonstrates Java 25 Stream Gatherers (JEP 485). Enables custom and built-in intermediate stream
 * transformations (windowing, folding, scanning).
 */
public class StreamGatherersDemo {

    /**
     * Partitions an input stream into fixed-size contiguous windows using Gatherers.windowFixed.
     */
    public static List<List<Integer>> fixedBatches(List<Integer> elements, int batchSize) {
        return elements.stream().gather(Gatherers.windowFixed(batchSize)).toList();
    }

    /** Creates a sliding window over elements using Gatherers.windowSliding. */
    public static List<List<Integer>> slidingWindows(List<Integer> elements, int windowSize) {
        return elements.stream().gather(Gatherers.windowSliding(windowSize)).toList();
    }

    /** Calculates cumulative running sum using Gatherers.scan. */
    public static List<Integer> runningSum(List<Integer> numbers) {
        return numbers.stream().gather(Gatherers.scan(() -> 0, (acc, next) -> acc + next)).toList();
    }
}
