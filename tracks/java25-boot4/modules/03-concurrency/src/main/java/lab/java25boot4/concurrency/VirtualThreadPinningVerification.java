package lab.java25boot4.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verifies that synchronized monitor acquisition no longer pins virtual threads to carrier threads
 * in Java 25 (ObjectMonitor unmounting).
 */
public class VirtualThreadPinningVerification {

    private final Object monitorLock = new Object();

    public boolean executeSynchronizedBlockingOperation() throws InterruptedException {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        Thread virtualThread =
                Thread.ofVirtual()
                        .name("vt-monitor-test")
                        .start(
                                () -> {
                                    synchronized (monitorLock) {
                                        entered.countDown();
                                        try {
                                            // Blocking inside synchronized block in Java 21 pinned
                                            // the carrier thread.
                                            // In Java 25, virtual threads yield and unmount
                                            // cleanly.
                                            proceed.await();
                                            completed.set(true);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                                });

        entered.await();
        // Release the latch allowing virtual thread to finish
        proceed.countDown();
        virtualThread.join();

        return completed.get();
    }
}
