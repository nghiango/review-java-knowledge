package lab.java25boot4.corejava;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the elimination of Virtual Thread Carrier Pinning on synchronized blocks in Java 25.
 * In Java 21, synchronized blocks pinned carrier threads; in Java 25, the JVM ObjectMonitor
 * unmounts virtual threads safely.
 */
public class VirtualThreadSynchronizedSafety {

    private final Object lock = new Object();
    private final AtomicInteger completedOperations = new AtomicInteger(0);

    public void executeSynchronizedWork(CountDownLatch latch) throws InterruptedException {
        synchronized (lock) {
            // In Java 25, blocking or sleeping inside synchronized does not pin the underlying
            // carrier thread!
            TimeUnit.MILLISECONDS.sleep(10);
            completedOperations.incrementAndGet();
            latch.countDown();
        }
    }

    public int getCompletedOperations() {
        return completedOperations.get();
    }
}
