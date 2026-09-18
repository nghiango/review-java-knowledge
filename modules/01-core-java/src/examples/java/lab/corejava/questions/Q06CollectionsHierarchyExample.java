package lab.corejava.questions;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public final class Q06CollectionsHierarchyExample {
    private Q06CollectionsHierarchyExample() {}

    public static void main(String[] args) {
        List<String> list = List.of("A", "B", "A");
        int listSize = list.size(); // 3 (preserves insertion order and duplicates)

        Set<String> set = Set.of("A", "B");
        int setSize = set.size(); // 2 (enforces unique elements)

        Map<String, Integer> map = Map.of("A", 1, "B", 2);
        int mapValue = map.get("A"); // 1 (associates unique keys to values)
    }
}
