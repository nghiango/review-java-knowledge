package lab.concurrency.broken.asyncpipeline;

public class OrderHistoryClient {
    public int fetchOrderCount(String customerId) {
        try {
            // Simulated slow remote database query
            Thread.sleep(120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 5;
    }
}
