package lab.concurrency.questions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/** Q13: Demonstrates CountDownLatch vs CyclicBarrier vs Semaphore. */
@SuppressWarnings("unused")
public class Q13CoordinationUtilitiesExample {

    public static void main(String[] args) throws InterruptedException {
        // CountDownLatch: one-shot barrier for start/finish synchronization
        CountDownLatch latch = new CountDownLatch(2);
        latch.countDown();
        long remainingCount = latch.getCount(); // 1
        latch.countDown();
        boolean passed = latch.await(10, java.util.concurrent.TimeUnit.MILLISECONDS); // true

        // Semaphore: permits controlling concurrent access to a bounded resource pool
        Semaphore semaphore = new Semaphore(2);
        boolean acquiredFirst = semaphore.tryAcquire(); // true
        boolean acquiredSecond = semaphore.tryAcquire(); // true
        boolean acquiredThird = semaphore.tryAcquire(); // false (no permits available)

        semaphore.release();
        int availablePermits = semaphore.availablePermits(); // 1
    }
}
