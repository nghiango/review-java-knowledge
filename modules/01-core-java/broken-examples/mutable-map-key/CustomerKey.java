package lab.corejava.broken.mutablemapkey;

import java.util.List;
import java.util.Objects;

public class CustomerKey {
    private final String tenantId;
    private String customerId;
    private String displayName;
    private final List<String> tags;

    public CustomerKey(String tenantId, String customerId, String displayName, List<String> tags) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.displayName = displayName;
        this.tags = tags;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object candidate) {
        if (!(candidate instanceof CustomerKey other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(customerId, other.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, customerId, displayName);
    }
}
