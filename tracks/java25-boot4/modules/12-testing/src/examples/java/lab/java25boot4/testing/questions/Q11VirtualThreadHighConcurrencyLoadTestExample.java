package lab.java25boot4.testing.questions;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q11: How do you design high-concurrency stress tests for virtual threads without pinning carrier
 * threads?
 */
public class Q11VirtualThreadHighConcurrencyLoadTestExample {

    public static void main(String[] args) throws Exception {
        int tasks = 1_000;
        AtomicInteger counter = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(
                        () -> {
                            counter.incrementAndGet();
                            return null;
                        });
            }
        }

        System.out.println(
                "All virtual tasks finished: "
                        + (counter.get() == tasks)); // All virtual tasks finished: true
    }
}
