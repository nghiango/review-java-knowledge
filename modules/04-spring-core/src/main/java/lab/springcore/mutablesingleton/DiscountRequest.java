package lab.springcore.mutablesingleton;

public record DiscountRequest(String customerId, double orderTotal, int loyaltyPoints) {
    public DiscountRequest {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (orderTotal < 0) {
            throw new IllegalArgumentException("orderTotal cannot be negative");
        }
    }
}
