package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q29SafepointLoopStallScenarioExample {
    private Q29SafepointLoopStallScenarioExample() {}

    // Problematic counted loop without safepoint poll in older JIT versions:
    // C2 compiler historically unrolls and strips safepoint polls from standard `int` counted loops
    // to maximize loop execution speed.
    public static long calculateHash(byte[] data) {
        long hash = 0;
        // If this loop runs for tens of milliseconds without method calls,
        // and a GC safepoint is requested by another thread, the entire JVM STW pause
        // is delayed until this thread reaches the end of the loop!
        for (int i = 0; i < data.length; i++) {
            hash = 31 * hash + data[i];
        }
        return hash;
    }

    public static void main(String[] args) {
        byte[] dummy = new byte[] {1, 2, 3, 4, 5};
        long result = calculateHash(dummy); // 3267770L

        // Java 10+ HotSpot feature: Loop Strip Mining (-XX:+UseCountedLoopSafepoints)
        // Splits the loop into an inner vectorizable loop and an outer loop with safepoints,
        // eliminating "Time To Safepoint" (TTSP) p99 latency stalls while maintaining peak throughput.
    }
}
