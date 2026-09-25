package lab.corejava.questions;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;

@SuppressWarnings("unused")
public final class Q25SequencedCollectionsExample {
    private Q25SequencedCollectionsExample() {}

    public static void main(String[] args) {
        // SequencedCollection: well-defined first and last elements
        SequencedCollection<String> deque = new ArrayDeque<>(List.of("alpha", "beta", "gamma"));
        String first = deque.getFirst(); // "alpha"
        String last = deque.getLast(); // "gamma"

        deque.addFirst("zero");
        deque.addLast("omega");

        // reversed() view - operates in reverse order without copying underlying elements
        SequencedCollection<String> reversed = deque.reversed();
        String revFirst = reversed.getFirst(); // "omega"

        // SequencedSet
        SequencedSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        set.addFirst(0); // [0, 1, 2, 3]
        set.addLast(4); // [0, 1, 2, 3, 4]

        // SequencedMap
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.putLast("one", 1);
        map.putLast("two", 2);
        map.putFirst("zero", 0);

        String firstKey = map.firstEntry().getKey(); // "zero"
        String lastKey = map.lastEntry().getKey(); // "two"
        SequencedMap<String, Integer> reversedMap = map.reversed();
        String revKey = reversedMap.firstEntry().getKey(); // "two"
    }
}
