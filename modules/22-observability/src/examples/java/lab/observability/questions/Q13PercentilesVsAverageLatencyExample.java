package lab.observability.questions;

import java.util.Arrays;

public class Q13PercentilesVsAverageLatencyExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Arithmetic mean masks tail latency spikes:
        // Suppose 99 requests take 10ms and 1 request takes 10,000ms.
        long[] latencies = new long[100];
        Arrays.fill(latencies, 0, 99, 10L);
        latencies[99] = 10000L;

        double sum = 0;
        for (long l : latencies) {
            sum += l;
        }
        double average = sum / latencies.length; // 109.9ms

        // Percentiles expose true customer experience:
        Arrays.sort(latencies);
        long p50 = latencies[50]; // 10L
        long p99 = latencies[99]; // 10000L

        boolean averageHidesOutlier = average < 200.0 && p99 == 10000L; // true

        System.out.println("Arithmetic average latency: " + average + "ms");
        System.out.println("Median (p50) latency: " + p50 + "ms");
        System.out.println("Tail (p99) latency: " + p99 + "ms");
        System.out.println("Average hides catastrophic 10s stall: " + averageHidesOutlier);
    }
}
