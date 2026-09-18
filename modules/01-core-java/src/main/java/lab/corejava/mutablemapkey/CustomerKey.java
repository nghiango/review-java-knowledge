package lab.corejava.mutablemapkey;

public record CustomerKey(String tenantId, String customerId) {

    public CustomerKey {
        tenantId = requireText(tenantId, "tenantId");
        customerId = requireText(customerId, "customerId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
