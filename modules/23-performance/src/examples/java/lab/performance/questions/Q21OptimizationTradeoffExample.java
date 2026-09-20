package lab.performance.questions;

public final class Q21OptimizationTradeoffExample {
    public static void main(String[] args) {
        int savedMicros = 5;
        long callsPerSecond = 1_000_000;
        long cpuMicrosSaved = savedMicros * callsPerSecond; // 5 CPU-seconds/second
        System.out.println(cpuMicrosSaved);
    }
}
