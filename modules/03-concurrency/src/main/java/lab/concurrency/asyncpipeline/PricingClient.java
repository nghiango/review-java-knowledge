package lab.concurrency.asyncpipeline;

public class PricingClient {
    public double fetchPrice(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        return 19.99;
    }
}
