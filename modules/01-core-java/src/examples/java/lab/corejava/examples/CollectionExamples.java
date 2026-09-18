package lab.corejava.examples;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CollectionExamples {
    private CollectionExamples() {}

    public static List<String> orderedDuplicates() {
        return List.of("new", "new", "paid");
    }

    public static Set<String> uniqueStatuses() {
        return Set.of("new", "paid");
    }

    public static Map<String, String> customersById() {
        return Map.of("customer-42", "Ada");
    }

    public static String lookupEquivalentKey() {
        Map<EqualityExamples.CustomerKey, String> index = new HashMap<>();
        index.put(new EqualityExamples.CustomerKey("tenant-a", "customer-42"), "active");
        return index.get(new EqualityExamples.CustomerKey("tenant-a", "customer-42"));
    }

    public static List<String> insertNearFront() {
        var values = new ArrayList<>(List.of("first", "last"));
        values.add(1, "middle");
        return List.copyOf(values);
    }

    public static Set<String> sortedByLengthThenText() {
        var values =
                new TreeSet<>(
                        Comparator.comparingInt(String::length)
                                .thenComparing(Comparator.naturalOrder()));
        values.addAll(List.of("Ada", "Bob", "Lin"));
        return values;
    }
}
