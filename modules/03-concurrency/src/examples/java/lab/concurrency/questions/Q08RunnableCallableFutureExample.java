package lab.concurrency.questions;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Q08: Demonstrates Runnable vs Callable vs Future. */
@SuppressWarnings("unused")
public class Q08RunnableCallableFutureExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Runnable task1 =
                () -> {
                    int a = 1 + 1; // void return, cannot throw checked exceptions directly
                };

        Callable<String> task2 =
                () -> {
                    return "SUCCESS"; // returns value, can throw checked exceptions
                };

        Future<?> f1 = executor.submit(task1);
        Future<String> f2 = executor.submit(task2);

        Object f1Result = f1.get(); // null
        String f2Result = f2.get(); // "SUCCESS"

        executor.shutdown();
    }
}
