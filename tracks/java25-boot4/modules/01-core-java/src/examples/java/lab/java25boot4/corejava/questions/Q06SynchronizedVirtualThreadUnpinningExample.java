package lab.java25boot4.corejava.questions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Q06SynchronizedVirtualThreadUnpinningExample {

    public static void main(String[] args) throws InterruptedException {
        // Force a single carrier thread so pinning would be visible immediately.
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "1");

        Object lock = new Object();
        CountDownLatch holdsLock = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicLong secondRanAtMillis = new AtomicLong(-1);
        long start = System.nanoTime();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(
                    () -> {
                        synchronized (lock) {
                            holdsLock.countDown();
                            try {
                                // On Java 21 this pinned the carrier; on Java 24+ the virtual
                                // thread
                                // unmounts and the carrier is released.
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        done.countDown();
                    });

            holdsLock.await(1, TimeUnit.SECONDS);
            executor.submit(
                    () -> {
                        secondRanAtMillis.set((System.nanoTime() - start) / 1_000_000);
                        done.countDown();
                    });
        }

        done.await(3, TimeUnit.SECONDS);
        long secondRanAt = secondRanAtMillis.get();
        System.out.println(
                secondRanAt < 200); // true — the carrier was free while the first VT slept
    }
}
