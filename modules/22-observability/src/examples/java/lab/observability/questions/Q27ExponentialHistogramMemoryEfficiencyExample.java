package lab.observability.questions;

import java.util.Arrays;

/**
 * Demonstrates high-resolution histogram architectures: Explicit Buckets (fixed boundaries, higher series count)
 * versus Exponential Buckets (OpenTelemetry / Prometheus Native Histograms, dynamic resolution with reduced memory).
 */
public class Q27ExponentialHistogramMemoryEfficiencyExample {

    @SuppressWarnings("ArrayRecordComponent")
    record ExplicitHistogram(double[] upperBounds, long[] counts) {
        public int bucketCount() {
            return upperBounds.length;
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
    record ExponentialHistogram(int scale, int zeroCount, int[] positiveBuckets) {
        public int bucketCount() {
            return positiveBuckets.length;
        }
    }

    public static void main(String[] args) {
        // Traditional explicit buckets require e.g. 15-30 fixed series per histogram metric
        double[] fixedSloBoundaries = {0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0};
        ExplicitHistogram explicit = new ExplicitHistogram(fixedSloBoundaries, new long[fixedSloBoundaries.length]);

        // Native / exponential histograms automatically adapt bucket boundaries based on base 2^(2^-scale)
        // providing high relative accuracy with dense sparse-bucket arrays
        int scale = 6; // ~1.1% relative error
        ExponentialHistogram exponential = new ExponentialHistogram(scale, 0, new int[8]);

        boolean usesExplicitBoundaries = explicit.bucketCount() == fixedSloBoundaries.length; // true
        boolean usesDynamicScale = exponential.scale() == 6; // true

        System.out.println("Explicit buckets fixed count: " + usesExplicitBoundaries);
        System.out.println("Exponential histogram configured scale: " + usesDynamicScale);
    }
}
