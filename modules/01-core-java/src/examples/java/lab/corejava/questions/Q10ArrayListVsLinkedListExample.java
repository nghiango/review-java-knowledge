package lab.corejava.questions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"unused", "JdkObsolete"})
public final class Q10ArrayListVsLinkedListExample {
    private Q10ArrayListVsLinkedListExample() {}

    public static void main(String[] args) {
        List<String> arrayList = new ArrayList<>(List.of("A", "B", "C"));
        String indexedGet =
                arrayList.get(
                        1); // "B" (O(1) direct array index lookup with contiguous cache locality)

        List<String> linkedList = new LinkedList<>(List.of("A", "B", "C"));
        linkedList.addFirst(
                "START"); // O(1) pointer update at head, but node wrapper adds 24+ bytes per
        // element
        String first = linkedList.get(0); // "START"
    }
}
