package lab.java25boot4.corejava;

/**
 * Correct counterpart to {@code broken-examples/primitive-pattern-matching-loss}: wrapper types are
 * matched explicitly and every narrowing conversion is guarded, so a value can never be truncated
 * silently.
 */
public class SafeMetricConverter {

    public static String formatMetric(Object value) {
        return switch (value) {
            case Byte b -> "byte: " + b;
            case Short s -> "short: " + s;
            case Integer i -> "int: " + i;
            case Long l -> "long: " + l;
            case Double d -> "double: " + d;
            case null -> "null";
            default -> "unknown: " + value;
        };
    }

    public static int toSmallInt(Object value) {
        if (value instanceof Integer i) {
            if (i < Byte.MIN_VALUE || i > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("Integer exceeds byte capacity: " + i);
            }
            return i;
        }
        throw new IllegalArgumentException("Not an Integer: " + value);
    }
}
