package lab.java25boot4.resilience.questions;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Q08: How does synchronized blocking inside resilience decorators trigger carrier thread pinning
 * on Virtual Threads?
 */
public class Q08CarrierPinningResilienceTrapExample {

    static class CarrierSafeCircuitBreaker {
        private final ReentrantLock lock = new ReentrantLock();
        private int failureCount = 0;

        public void recordFailure() {
            // ReentrantLock parks virtual threads without pinning carrier OS threads
            lock.lock();
            try {
                failureCount++;
            } finally {
                lock.unlock();
            }
        }

        public int getFailureCount() {
            lock.lock();
            try {
                return failureCount;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        var breaker = new CarrierSafeCircuitBreaker();
        breaker.recordFailure();

        System.out.println(
                "Carrier safe breaker failure count: "
                        + breaker.getFailureCount()); // Carrier safe breaker failure count: 1
    }
}
