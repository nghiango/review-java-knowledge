package lab.java25boot4.resilience.questions;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q11: How do Virtual Threads handle thousands of concurrent resilient executions without thread
 * pool exhaustion?
 */
public class Q11HighThroughputVirtualThreadStressExample {

    public static void main(String[] args) throws Exception {
        var retryConfig = ModernResilientExecutionEngine.RetryConfig.defaultTransient(2);
        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(
                        50, java.time.Duration.ofSeconds(1));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 500);

        var completed = new AtomicInteger(0);
        int totalTasks = 100;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalTasks; i++) {
                executor.submit(
                        () -> {
                            engine.executeWithResilience(
                                    () -> {
                                        completed.incrementAndGet();
                                        return "OK";
                                    });
                        });
            }
        }

        System.out.println(
                "High concurrency tasks completed: "
                        + completed.get()); // High concurrency tasks completed: 100
    }
}
