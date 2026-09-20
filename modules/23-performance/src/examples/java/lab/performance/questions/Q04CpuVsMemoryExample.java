package lab.performance.questions;

public final class Q04CpuVsMemoryExample {
    public static void main(String[] args) {
        long liveBytes = 80;
        long heapBytes = 100;
        double occupancy = (double) liveBytes / heapBytes; // 0.8
        System.out.println(occupancy);
    }
}
