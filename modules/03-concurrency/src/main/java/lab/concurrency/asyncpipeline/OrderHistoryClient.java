package lab.concurrency.asyncpipeline;

public class OrderHistoryClient {
    public int fetchOrderCount(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        return 7;
    }
}
