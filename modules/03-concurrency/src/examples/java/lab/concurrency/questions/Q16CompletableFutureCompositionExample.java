package lab.concurrency.questions;

import java.util.concurrent.CompletableFuture;

/** Q16: Demonstrates CompletableFuture non-blocking composition and combining. */
@SuppressWarnings("unused")
public class Q16CompletableFutureCompositionExample {

    public static void main(String[] args) {
        CompletableFuture<String> stage1 = CompletableFuture.supplyAsync(() -> "hello");

        // thenApply transforms value synchronously on completion
        CompletableFuture<String> stage2 = stage1.thenApply(String::toUpperCase); // "HELLO"

        // thenCompose chains dependent asynchronous operations (flatMap)
        CompletableFuture<String> stage3 =
                stage2.thenCompose(val -> CompletableFuture.supplyAsync(() -> val + " WORLD"));

        // thenCombine merges two independent futures
        CompletableFuture<Integer> independent = CompletableFuture.supplyAsync(() -> 2026);
        CompletableFuture<String> combined =
                stage3.thenCombine(independent, (text, year) -> text + " " + year);

        String result = combined.join(); // "HELLO WORLD 2026"
    }
}
