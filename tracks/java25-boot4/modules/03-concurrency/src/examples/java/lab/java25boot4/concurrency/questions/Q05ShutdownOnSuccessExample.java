package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

public class Q05ShutdownOnSuccessExample {

    public static void main(String[] args) throws Throwable {
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(
                    () -> {
                        Thread.sleep(150);
                        return "Slow Node";
                    });

            scope.fork(
                    () -> {
                        Thread.sleep(20);
                        return "Fast Node";
                    });

            String winner = scope.join();
            System.out.println(winner); // Fast Node
        }
    }
}
