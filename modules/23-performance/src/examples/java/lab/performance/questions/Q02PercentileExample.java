package lab.performance.questions;

import java.util.Arrays;

public final class Q02PercentileExample {
    public static void main(String[] args) {
        long[] latency = {10, 11, 12, 13, 500};
        Arrays.sort(latency);
        long p100 = latency[4]; // 500 ms: the tail is invisible in a median
        System.out.println(p100);
    }
}
