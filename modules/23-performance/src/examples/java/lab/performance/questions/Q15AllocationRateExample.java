package lab.performance.questions;

public final class Q15AllocationRateExample {
    public static void main(String[] args) {
        long bytesPerRequest = 64_000, requestsPerSecond = 2_000;
        long bytesPerSecond = bytesPerRequest * requestsPerSecond; // 128 MB/s
        System.out.println(bytesPerSecond);
    }
}
