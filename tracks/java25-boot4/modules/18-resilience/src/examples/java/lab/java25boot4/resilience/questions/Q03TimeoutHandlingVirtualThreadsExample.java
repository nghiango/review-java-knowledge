package lab.java25boot4.resilience.questions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Q03: How should timeouts be enforced on Virtual Thread operations without blocking carrier
 * threads?
 */
public class Q03TimeoutHandlingVirtualThreadsExample {

    public static void main(String[] args) {
        CompletableFuture<String> slowCall =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                Thread.sleep(Duration.ofMillis(200));
                                return "SUCCESS";
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                            }
                        });

        // Enforce timeout
        CompletableFuture<String> guarded = slowCall.orTimeout(50, TimeUnit.MILLISECONDS);

        boolean timedOut = false;
        try {
            guarded.join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof TimeoutException) {
                timedOut = true;
            }
        }

        System.out.println(
                "Operation timed out cleanly: " + timedOut); // Operation timed out cleanly: true
    }
}
