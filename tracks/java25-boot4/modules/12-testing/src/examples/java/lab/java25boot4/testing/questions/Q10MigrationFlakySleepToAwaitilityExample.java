package lab.java25boot4.testing.questions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;

/**
 * Q10: How do you refactor brittle legacy test suites that rely on Thread.sleep() for asynchronous
 * verification?
 */
public class Q10MigrationFlakySleepToAwaitilityExample {

    public static void main(String[] args) {
        AtomicBoolean taskDone = new AtomicBoolean(false);

        Thread.startVirtualThread(
                () -> {
                    try {
                        Thread.sleep(15);
                        taskDone.set(true);
                    } catch (InterruptedException ignored) {
                    }
                });

        // Refactored: eliminates sleep(500) fixed latency and CI race conditions
        Awaitility.await()
                .atMost(Duration.ofMillis(300))
                .pollInterval(Duration.ofMillis(5))
                .untilTrue(taskDone);

        System.out.println(
                "Migrated task completed: " + taskDone.get()); // Migrated task completed: true
    }
}
