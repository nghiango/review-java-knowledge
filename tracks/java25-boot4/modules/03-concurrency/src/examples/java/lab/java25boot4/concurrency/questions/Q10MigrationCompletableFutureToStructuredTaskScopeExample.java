package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class Q10MigrationCompletableFutureToStructuredTaskScopeExample {

    public static void main(String[] args) throws Throwable {
        // Replacing CompletableFuture.allOf with
        // StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<String> account = scope.fork(() -> "ACC-01");
            Subtask<Double> balance = scope.fork(() -> 1500.50);

            scope.join();

            System.out.println("Account: " + account.get()); // Account: ACC-01
            System.out.println("Balance: " + balance.get()); // Balance: 1500.5
        }
    }
}
