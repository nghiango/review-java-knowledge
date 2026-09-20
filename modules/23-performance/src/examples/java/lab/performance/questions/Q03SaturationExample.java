package lab.performance.questions;

public final class Q03SaturationExample {
    public static void main(String[] args) {
        int busyWorkers = 8;
        int workers = 8;
        boolean saturated = busyWorkers == workers; // true
        System.out.println(saturated);
    }
}
