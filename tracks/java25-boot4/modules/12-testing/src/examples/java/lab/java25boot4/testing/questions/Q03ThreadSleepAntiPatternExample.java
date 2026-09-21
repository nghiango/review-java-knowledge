package lab.java25boot4.testing.questions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;

/**
 * Q03: Why is Thread.sleep considered an anti-pattern in asynchronous and virtual thread test
 * suites?
 */
public class Q03ThreadSleepAntiPatternExample {

    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger(0);

        Thread.startVirtualThread(
                () -> {
                    try {
                        Thread.sleep(20);
                        counter.set(42);
                    } catch (InterruptedException ignored) {
                    }
                });

        // Use Awaitility instead of arbitrary Thread.sleep(200)
        Awaitility.await()
                .atMost(Duration.ofMillis(500))
                .pollInterval(Duration.ofMillis(10))
                .until(() -> counter.get() == 42);

        System.out.println("Counter reached: " + counter.get()); // Counter reached: 42
    }
}
