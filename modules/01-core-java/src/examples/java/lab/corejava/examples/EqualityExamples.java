package lab.corejava.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EqualityExamples {
    private EqualityExamples() {}

    public record CustomerKey(String tenantId, String customerId) {}

    public record CustomerSnapshot(CustomerKey key, List<String> tags) {
        public CustomerSnapshot {
            tags = List.copyOf(tags);
        }
    }

    public static boolean equalKeysFindTheSameEntry() {
        Map<CustomerKey, String> cache = new HashMap<>();
        cache.put(new CustomerKey("tenant-a", "customer-42"), "active");
        return "active".equals(cache.get(new CustomerKey("tenant-a", "customer-42")));
    }

    public static CustomerSnapshot detachedSnapshot(List<String> mutableTags) {
        return new CustomerSnapshot(new CustomerKey("tenant-a", "customer-42"), mutableTags);
    }
}
