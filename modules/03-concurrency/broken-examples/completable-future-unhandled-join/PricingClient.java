package lab.concurrency.broken.asyncpipeline;

public class PricingClient {
    public double fetchPrice(String itemId) {
        try {
            // Simulated slow remote REST pricing call
            Thread.sleep(80);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 19.99;
    }
}
