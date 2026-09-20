package lab.performance.questions;

public final class Q13GcTailLatencyExample {
    public static void main(String[] args) {
        long appMs = 20, pauseMs = 180;
        long observedMs = appMs + pauseMs; // 200 ms request latency
        System.out.println(observedMs);
    }
}
