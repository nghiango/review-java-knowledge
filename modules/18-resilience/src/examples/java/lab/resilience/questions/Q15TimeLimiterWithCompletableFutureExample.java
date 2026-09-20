package lab.resilience.questions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Q15TimeLimiterWithCompletableFutureExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // TimeLimiter operates on CompletionStage / CompletableFuture or Future
        Duration timeout = Duration.ofSeconds(1);
        CompletableFuture<String> asyncTask =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            return "COMPLETED";
                        });

        String result = asyncTask.join();
        boolean completedWithinDeadline = "COMPLETED".equals(result); // true

        System.out.println("Async call finished within deadline: " + completedWithinDeadline);
    }
}
