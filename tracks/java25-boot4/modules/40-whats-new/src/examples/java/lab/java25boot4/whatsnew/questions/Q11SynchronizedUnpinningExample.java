package lab.java25boot4.whatsnew.questions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Q11: does {@code synchronized} still pin a carrier thread on Java 25? */
public class Q11SynchronizedUnpinningExample {

    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(5);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                executor.submit(
                        () -> {
                            synchronized (lock) { // blocking inside a monitor
                                try {
                                    Thread.sleep(10); // JEP 491: this does NOT pin the carrier on Java 25
                                } catch (InterruptedException interrupted) {
                                    Thread.currentThread().interrupt();
                                }
                                completed.incrementAndGet();
                                latch.countDown();
                            }
                        });
            }
            latch.await(5, TimeUnit.SECONDS);
        }

        System.out.println(completed.get()); // 5 — all virtual threads made progress
    }
}
