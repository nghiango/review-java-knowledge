package lab.concurrency.questions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused")
public final class Q29ThreadPoolStarvationDeadlockScenarioExample {
    private Q29ThreadPoolStarvationDeadlockScenarioExample() {}

    public static void main(String[] args) throws Exception {
        // Pool of size 2
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Submitting 2 parent tasks that occupy both pool workers:
        Future<?> parent1 = pool.submit(() -> {
            // Parent submits subtask to the SAME pool and blocks waiting for it:
            Future<String> subtask = pool.submit(() -> "subtask-1");
            try {
                // If the pool has no available threads, subtask sits in queue forever!
                // Deadlock: parent holds thread waiting for subtask; subtask needs thread held by parent!
                return subtask.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                return "timed-out-due-to-starvation";
            } catch (Exception e) {
                return "error";
            }
        });

        Object outcome = parent1.get(); // "timed-out-due-to-starvation"
        pool.shutdownNow();
    }
}
