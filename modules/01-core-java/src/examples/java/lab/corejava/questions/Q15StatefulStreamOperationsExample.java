package lab.corejava.questions;

import java.util.List;

@SuppressWarnings("unused")
public final class Q15StatefulStreamOperationsExample {
    private Q15StatefulStreamOperationsExample() {}

    public static void main(String[] args) {
        List<String> items = List.of("banana", "apple", "cherry", "apple");

        // map() is stateless (element-by-element); distinct() and sorted() are stateful (barrier)
        List<String> processed =
                items.stream()
                        .map(String::toUpperCase) // stateless
                        .distinct() // stateful (tracks seen elements)
                        .sorted() // stateful (buffers entire upstream before sorting)
                        .toList();

        int size = processed.size(); // 3
        String first = processed.get(0); // "APPLE"
        String second = processed.get(1); // "BANANA"
    }
}
