package lab.springboot.questions;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class Q30RefreshScopeDynamicConfigScenarioExample {
    private Q30RefreshScopeDynamicConfigScenarioExample() {}

    // Simulated @RefreshScope bean:
    // Beans in RefreshScope are wrapped in a CGLIB proxy backed by a target source cache.
    // When a /actuator/refresh event occurs, the target cache is evicted, and the next call
    // creates a fresh bean instance with updated configuration properties.
    public static class DynamicPricingService {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private volatile double discountRate;

        public DynamicPricingService(double discountRate) {
            this.discountRate = discountRate;
        }

        public double calculatePrice(double basePrice) {
            executionCount.incrementAndGet();
            return basePrice * (1.0 - discountRate);
        }

        public void updateDiscount(double newRate) {
            this.discountRate = newRate;
        }

        public double getDiscountRate() { return discountRate; }
    }

    public static void main(String[] args) {
        DynamicPricingService service = new DynamicPricingService(0.10);
        double price1 = service.calculatePrice(100.0); // 90.0

        // Dynamic refresh event updates properties:
        service.updateDiscount(0.20);
        double price2 = service.calculatePrice(100.0); // 80.0

        // Pitfall: If an asynchronous or thread-pool task retains a reference to the inner target bean
        // rather than invoking through the RefreshScope proxy, it continues reading stale configuration!
    }
}
