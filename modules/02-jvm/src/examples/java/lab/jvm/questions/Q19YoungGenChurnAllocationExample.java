package lab.jvm.questions;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class Q19YoungGenChurnAllocationExample {
    private Q19YoungGenChurnAllocationExample() {}

    // Precompiled Pattern: compiled once during class init, zero regex parsing allocations on hot
    // path
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public static String formatMetric(String name, long value) {
        if (!PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid name");
        }
        // Pre-sized StringBuilder avoids intermediate wrapper and stream allocations
        StringBuilder sb = new StringBuilder(name.length() + 16);
        sb.append(name).append('=').append(value);
        return sb.toString(); // "cpu_load=85"
    }

    public static void main(String[] args) {
        String result = formatMetric("cpu_load", 85L); // "cpu_load=85"
    }
}
