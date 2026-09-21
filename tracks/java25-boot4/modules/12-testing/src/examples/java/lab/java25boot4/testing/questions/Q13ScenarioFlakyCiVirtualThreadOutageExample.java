package lab.java25boot4.testing.questions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Q13: Incident scenario: Flaky CI test suites timing out after virtual thread migration. */
public class Q13ScenarioFlakyCiVirtualThreadOutageExample {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        Thread.startVirtualThread(
                () -> {
                    completed.set(true);
                    latch.countDown();
                });

        // Use deterministic latch await instead of Thread.sleep
        boolean reached = latch.await(1, TimeUnit.SECONDS);

        System.out.println(
                "Synchronized deterministically without CI race condition: "
                        + (reached
                                && completed
                                        .get())); // Synchronized deterministically without CI race
        // condition: true
    }
}
