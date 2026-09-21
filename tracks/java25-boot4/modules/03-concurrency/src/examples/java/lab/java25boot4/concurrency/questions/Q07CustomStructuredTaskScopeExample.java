package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.atomic.AtomicInteger;

public class Q07CustomStructuredTaskScopeExample {

    // Custom Joiner that counts completed tasks
    static class CountingJoiner implements Joiner<Integer, Integer> {
        private final AtomicInteger successCount = new AtomicInteger(0);

        @Override
        public boolean onComplete(Subtask<? extends Integer> subtask) {
            if (subtask.state() == Subtask.State.SUCCESS) {
                successCount.incrementAndGet();
            }
            return false;
        }

        @Override
        public Integer result() {
            return successCount.get();
        }
    }

    public static void main(String[] args) throws Throwable {
        var joiner = new CountingJoiner();
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(() -> 10);
            scope.fork(() -> 20);
            scope.fork(
                    () -> {
                        throw new RuntimeException("Ignored failure");
                    });

            Integer count = scope.join();
            System.out.println(count); // 2
        }
    }
}
