package lab.corejava.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q07GenericsPecsExample {
    private Q07GenericsPecsExample() {}

    // Producer Extends (read from src), Consumer Super (write to dest)
    public static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) {
            dest.add(item);
        }
    }

    public static void main(String[] args) {
        List<Integer> integers = List.of(1, 2, 3);
        List<Number> numbers = new ArrayList<>();

        copy(integers, numbers);

        int size = numbers.size(); // 3
        Number first = numbers.get(0); // 1
    }
}
