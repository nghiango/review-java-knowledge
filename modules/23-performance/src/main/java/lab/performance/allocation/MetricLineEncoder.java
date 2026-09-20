package lab.performance.allocation;

import java.util.Comparator;
import java.util.Objects;

public final class MetricLineEncoder {
    public String encode(MetricEvent event) {
        Objects.requireNonNull(event, "event");
        // Operation-local mutable state avoids synchronization and cross-request data leakage.
        StringBuilder line = new StringBuilder(event.name()).append('{');
        event.tags().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey()))
                .forEachOrdered(
                        entry -> {
                            if (line.charAt(line.length() - 1) != '{') {
                                line.append(',');
                            }
                            appendEscaped(line, entry.getKey());
                            line.append('=');
                            appendEscaped(line, entry.getValue());
                        });
        return line.append("}=").append(event.value()).toString();
    }

    private static void appendEscaped(StringBuilder target, String value) {
        Objects.requireNonNull(value, "tag component");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\' || character == ',' || character == '=') {
                target.append('\\');
            }
            target.append(character);
        }
    }
}
