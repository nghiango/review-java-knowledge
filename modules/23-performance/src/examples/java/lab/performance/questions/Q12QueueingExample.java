package lab.performance.questions;

public final class Q12QueueingExample {
    public static void main(String[] args) {
        int capacity = 100, queued = 100;
        boolean reject = queued >= capacity; // true: apply backpressure
        System.out.println(reject);
    }
}
