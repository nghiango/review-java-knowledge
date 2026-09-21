package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class Q04ShutdownOnFailureExample {

    public static void main(String[] args) {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<String> task1 =
                    scope.fork(
                            () -> {
                                Thread.sleep(200);
                                return "Completed Task 1";
                            });

            Subtask<String> task2 =
                    scope.fork(
                            () -> {
                                throw new IllegalStateException("Subtask 2 failed immediately");
                            });

            scope.join();
        } catch (Throwable e) {
            System.out.println(e.getClass().getSimpleName()); // IllegalStateException
            System.out.println(e.getMessage()); // Subtask 2 failed immediately
        }
    }
}
