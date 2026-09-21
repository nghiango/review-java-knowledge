package lab.java25boot4.resilience.questions;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q02: Why are Semaphore-based bulkheads preferred over thread pool isolation when running on
 * Virtual Threads?
 */
public class Q02VirtualThreadBulkheadMechanicsExample {

    public static void main(String[] args) {
        Semaphore bulkhead = new Semaphore(2);
        AtomicInteger activeCount = new AtomicInteger(0);

        boolean permit1 = bulkhead.tryAcquire();
        boolean permit2 = bulkhead.tryAcquire();
        boolean permit3 = bulkhead.tryAcquire(); // Exceeds capacity

        System.out.println("Permit 1 acquired: " + permit1); // Permit 1 acquired: true
        System.out.println("Permit 2 acquired: " + permit2); // Permit 2 acquired: true
        System.out.println("Permit 3 acquired: " + permit3); // Permit 3 acquired: false

        if (permit1) bulkhead.release();
        if (permit2) bulkhead.release();

        System.out.println(
                "Available permits after release: "
                        + bulkhead.availablePermits()); // Available permits after release: 2
    }
}
