package lab.springcore.mutablesingleton;

import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Pure stateless Spring service for calculating promotional discounts. All state is confined to
 * method parameters and stack-allocated records, guaranteeing zero race conditions under concurrent
 * multi-threaded execution.
 */
@Service
public class DiscountCalculationService {

    public DiscountResult calculateDiscount(DiscountRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        double tierDiscount = computeTierDiscount(request.orderTotal());
        double loyaltyDiscount = computeLoyaltyDiscount(request.loyaltyPoints());
        double totalDiscount = tierDiscount + loyaltyDiscount;

        return new DiscountResult(
                request.customerId(), totalDiscount, tierDiscount, loyaltyDiscount);
    }

    private double computeTierDiscount(double orderTotal) {
        if (orderTotal > 500.0) {
            return orderTotal * 0.10;
        }
        return orderTotal * 0.05;
    }

    private double computeLoyaltyDiscount(int loyaltyPoints) {
        if (loyaltyPoints > 1000) {
            return 20.0;
        }
        return 5.0;
    }
}
