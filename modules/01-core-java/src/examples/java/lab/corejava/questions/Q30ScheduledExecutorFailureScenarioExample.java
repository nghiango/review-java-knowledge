package lab.corejava.questions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class Q30ScheduledExecutorFailureScenarioExample {
    private Q30ScheduledExecutorFailureScenarioExample() {
    }

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger counter = new AtomicInteger(0);

        // Flawed periodic task: unhandled exception causes subsequent executions to halt silently!
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            int current = counter.incrementAndGet();
            if (current == 3) {
                throw new RuntimeException("Unexpected transient error"); // Swallowed! Periodic schedule halts!
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        // Proper robust wrapper: catches all Throwables inside the runnable
        ScheduledFuture<?> robustFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Task business logic
            } catch (Throwable t) {
                // Log and record alert metric, preventing cancellation of subsequent runs
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        scheduler.shutdown();
    }
}
