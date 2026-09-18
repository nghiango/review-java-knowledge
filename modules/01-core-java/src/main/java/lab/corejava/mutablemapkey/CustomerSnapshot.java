package lab.corejava.mutablemapkey;

import java.util.List;
import java.util.Objects;

public record CustomerSnapshot(CustomerKey key, String displayName, List<String> tags) {

    public CustomerSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(tags, "tags");
        tags = List.copyOf(tags);
    }
}
