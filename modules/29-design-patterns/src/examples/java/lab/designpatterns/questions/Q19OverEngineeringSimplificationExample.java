package lab.designpatterns.questions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Q19: Refactoring Over-Engineered Patterns to Idiomatic Java 21. Demonstrates replacing cumbersome
 * Abstract Factory/Visitor hierarchies with straightforward stream joins.
 */
public class Q19OverEngineeringSimplificationExample {

    public record Metric(String name, double value) {}

    // Over-engineered approach requires Factory + Visitor + Builder (40+ lines)
    // Idiomatic Java 21 approach:
    public static String formatMetricsToCsv(List<Metric> metrics) {
        return metrics.stream()
                .map(m -> m.name() + "," + m.value())
                .collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        List<Metric> list = List.of(new Metric("cpu", 0.45), new Metric("mem", 0.78));

        String csv = formatMetricsToCsv(list);
        boolean clean = csv.equals("cpu,0.45\nmem,0.78"); // true

        System.out.println("Q19 clean: " + clean);
    }
}
