package lab.concurrency.questions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Q22 Scenario: Diagnosing thread pool self-deadlock and resolving with dedicated decoupled
 * executors.
 */
@SuppressWarnings("unused")
public class Q22ThreadPoolSelfDeadlockScenarioExample {

    public static void main(String[] args)
            throws ExecutionException, InterruptedException, TimeoutException {
        // Starvation risk: parent tasks and child tasks sharing a bounded pool of size 2
        ExecutorService parentPool = Executors.newFixedThreadPool(2);
        ExecutorService childPool =
                Executors.newFixedThreadPool(4); // Separate pool prevents self-deadlock

        Future<String> parentTask =
                parentPool.submit(
                        () -> {
                            // Submit sub-tasks to decoupled pool
                            Future<Integer> sub1 = childPool.submit(() -> 40);
                            Future<Integer> sub2 = childPool.submit(() -> 2);

                            return "Total=" + (sub1.get() + sub2.get());
                        });

        String result = parentTask.get(2, TimeUnit.SECONDS); // "Total=42"

        parentPool.shutdown();
        childPool.shutdown();
    }
}
