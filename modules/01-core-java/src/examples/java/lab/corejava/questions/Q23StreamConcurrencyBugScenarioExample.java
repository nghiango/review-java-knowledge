package lab.corejava.questions;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class Q23StreamConcurrencyBugScenarioExample {
    private Q23StreamConcurrencyBugScenarioExample() {}

    public static void main(String[] args) {
        List<Integer> inputs = List.of(1, 2, 3, 4, 5);

        // Concurrency fix: Use standard thread-safe reduction/collectors instead of unsynchronized
        // shared mutable list
        List<Integer> squared =
                inputs.parallelStream().map(n -> n * n).collect(Collectors.toList());

        int size = squared.size(); // 5 (no dropped rows or race conditions)
    }
}
