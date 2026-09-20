package lab.performance.allocation;

import java.util.Map;
import java.util.Objects;

public record MetricEvent(String name, Map<String, String> tags, long value) {
    public MetricEvent {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        tags = Map.copyOf(tags);
    }
}
