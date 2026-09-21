package lab.java25boot4.corejava.broken.patterns;

public class MetricConverter {

    public String formatMetric(Object value) {
        if (value instanceof byte b) {
            return "byte: " + b;
        } else if (value instanceof int i) {
            return "int: " + i;
        } else if (value instanceof long l) {
            return "long: " + l;
        } else if (value instanceof double d) {
            return "double: " + d;
        }
        return "unknown: " + value;
    }

    public int castToSmallInt(Object value) {
        if (value instanceof int i) {
            return (byte) i;
        }
        return -1;
    }
}
