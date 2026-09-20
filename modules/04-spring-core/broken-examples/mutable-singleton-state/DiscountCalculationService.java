package lab.springcore.broken.mutablesingleton;

import org.springframework.stereotype.Service;

@Service
public class DiscountCalculationService {

    // Anti-pattern: Storing per-request calculation state in instance fields of a Singleton Spring bean
    private String currentCustomerId;
    private double orderTotal;
    private int loyaltyPoints;

    public double calculateDiscount(String customerId, double total, int points) {
        this.currentCustomerId = customerId;
        this.orderTotal = total;
        this.loyaltyPoints = points;

        // Simulated processing window allowing concurrent requests to overwrite instance fields
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return computeTierDiscount() + computeLoyaltyDiscount();
    }

    private double computeTierDiscount() {
        if (orderTotal > 500) {
            return orderTotal * 0.10;
        }
        return orderTotal * 0.05;
    }

    private double computeLoyaltyDiscount() {
        if (loyaltyPoints > 1000) {
            return 20.0;
        }
        return 5.0;
    }

    public String getCurrentCustomerId() {
        return currentCustomerId;
    }
}
