package lab.concurrency.questions;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Q14: Demonstrates ThreadPoolExecutor core/max sizing and CallerRunsPolicy backpressure. */
@SuppressWarnings({"unused", "FutureReturnValueIgnored"})
public class Q14ThreadPoolRejectionPoliciesExample {

    public static void main(String[] args) {
        AtomicInteger callerThreadRuns = new AtomicInteger(0);
        String mainThreadName = Thread.currentThread().getName(); // "main"

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1), // Queue capacity = 1
                        new ThreadPoolExecutor.CallerRunsPolicy());

        // Fill core worker
        executor.submit(
                () -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        // Fill queue
        executor.submit(() -> {});

        // Third task overflows pool + queue: CallerRunsPolicy executes it on the calling thread
        // ("main")
        executor.submit(
                () -> {
                    if (Thread.currentThread().getName().equals(mainThreadName)) {
                        callerThreadRuns.incrementAndGet();
                    }
                });

        int executedOnCaller = callerThreadRuns.get(); // 1 (throttled producer)
        executor.shutdown();
    }
}
