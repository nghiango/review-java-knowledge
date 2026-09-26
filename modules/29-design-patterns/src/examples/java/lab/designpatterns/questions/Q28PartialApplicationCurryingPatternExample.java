package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Currying / Partial Application Pattern in modern Java, pre-binding configuration
 * parameters to high-order functions to construct specialized business validation functions.
 */
public class Q28PartialApplicationCurryingPatternExample {

    @FunctionalInterface
    public interface DiscountCalculator {
        double calculate(double basePrice);
    }

    public static class DiscountEngine {
        // High-order currying factory
        public static DiscountCalculator createDiscountCalculator(double percentageRate, double maxCap) {
            return basePrice -> {
                double discount = basePrice * percentageRate;
                return Math.min(discount, maxCap);
            };
        }
    }

    public static void main(String[] args) {
        // Partially applied functions: reusable discount strategies with fixed parameters
        DiscountCalculator vipDiscount = DiscountEngine.createDiscountCalculator(0.20, 50.0);

        double discountOn100 = vipDiscount.calculate(100.0); // 20.0
        double discountOn500 = vipDiscount.calculate(500.0); // 50.0 (capped)

        boolean calculatedCorrectly = (discountOn100 == 20.0) && (discountOn500 == 50.0); // true
        System.out.println("VIP discount on 100: " + discountOn100);
        System.out.println("VIP discount on 500: " + discountOn500);
        System.out.println("Partial application logic validated: " + calculatedCorrectly);
    }
}
