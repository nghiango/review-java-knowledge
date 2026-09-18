package lab.corejava.questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("unused")
public final class Q11ComparableVsComparatorExample {
    private Q11ComparableVsComparatorExample() {}

    public record Employee(String name, int age) implements Comparable<Employee> {
        @Override
        public int compareTo(Employee other) {
            return this.name.compareTo(other.name); // natural order: alphabetical by name
        }
    }

    public static void main(String[] args) {
        List<Employee> list =
                new ArrayList<>(List.of(new Employee("Bob", 30), new Employee("Alice", 25)));

        Collections.sort(list); // uses Comparable natural order
        String firstNatural = list.get(0).name(); // "Alice"

        list.sort(Comparator.comparingInt(Employee::age)); // uses custom Comparator by age
        String firstCustom = list.get(0).name(); // "Alice" (age 25)
    }
}
