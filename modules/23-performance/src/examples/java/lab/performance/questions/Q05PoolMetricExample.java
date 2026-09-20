package lab.performance.questions;

public final class Q05PoolMetricExample {
    public static void main(String[] args) {
        int active = 10, maximum = 10, pending = 25;
        boolean exhausted = active == maximum && pending > 0; // true
        System.out.println(exhausted);
    }
}
