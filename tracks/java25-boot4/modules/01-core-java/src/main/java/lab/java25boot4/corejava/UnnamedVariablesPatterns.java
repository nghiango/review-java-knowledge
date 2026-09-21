package lab.java25boot4.corejava;

import java.util.Map;

/**
 * Demonstrates Java 25 Unnamed Variables and Patterns (JEP 456).
 * Replaces unused parameters, loop targets, and catch variables with `_`.
 */
public class UnnamedVariablesPatterns {

    public record Coordinate(int x, int y, int z) {}

    public static int countValidCoordinates(Iterable<Coordinate> coordinates) {
        int count = 0;
        // Unnamed loop variable when element reference is not directly used
        for (Coordinate _ : coordinates) {
            count++;
        }
        return count;
    }

    public static String describeProjection(Coordinate coord) {
        // Record pattern matching with unnamed pattern variables
        if (coord instanceof Coordinate(int x, int y, _)) {
            return "2D projection at x=" + x + ", y=" + y;
        }
        return "unknown";
    }

    public static boolean parseOrIgnore(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException _) {
            // Unnamed exception variable where exception object is intentionally unused
            return false;
        }
    }

    public static int sumValues(Map<String, Integer> map) {
        int total = 0;
        // BiConsumer / lambda with unused key
        map.forEach((_, value) -> {
            // Unused key parameter indicated by _
        });
        for (var entry : map.entrySet()) {
            total += entry.getValue();
        }
        return total;
    }
}
