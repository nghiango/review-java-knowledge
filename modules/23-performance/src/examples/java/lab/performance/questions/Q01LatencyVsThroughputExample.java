package lab.performance.questions;

public final class Q01LatencyVsThroughputExample {
    public static void main(String[] args) {
        long requests = 1_000;
        long seconds = 10;
        long throughput = requests / seconds; // 100 requests/second
        System.out.println(throughput);
    }
}
