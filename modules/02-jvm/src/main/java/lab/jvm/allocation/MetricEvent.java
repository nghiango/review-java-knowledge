package lab.jvm.allocation;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record MetricEvent(String name, Map<String, String> tags, long value) {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    public MetricEvent {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank() || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid metric name: " + name);
        }
        Objects.requireNonNull(tags, "tags must not be null");
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "tag key must not be null");
            Objects.requireNonNull(entry.getValue(), "tag value must not be null");
            if (entry.getKey().isBlank()) {
                throw new IllegalArgumentException("tag key must not be blank");
            }
        }
        tags = Map.copyOf(tags);
    }
}
