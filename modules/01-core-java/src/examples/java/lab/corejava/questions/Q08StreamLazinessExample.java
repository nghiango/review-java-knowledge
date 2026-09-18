package lab.corejava.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q08StreamLazinessExample {
    private Q08StreamLazinessExample() {}

    public static void main(String[] args) {
        List<String> visited = new ArrayList<>();

        var stream =
                List.of("apple", "banana", "cherry", "date").stream()
                        .filter(
                                s -> {
                                    visited.add(s);
                                    return s.length() > 4;
                                })
                        .map(String::toUpperCase);

        int countBeforeTerminal = visited.size(); // 0 (intermediate stages do not execute)

        String first = stream.findFirst().orElse(""); // "APPLE"
        int countAfterTerminal = visited.size(); // 1 (lazy traversal terminates at first match)
    }
}
