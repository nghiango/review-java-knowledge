package lab.designpatterns.questions;

import java.util.Map;

/**
 * Q17: Refactoring the Switch-on-Type Anti-Pattern using Map-based Strategy Registry. Demonstrates
 * eliminating fragile OCP-violating switches with an extensible Map registry.
 */
public class Q17AntiPatternSwitchOnTypeExample {

    public interface FeeCalculator {
        double calculate(double amount);
    }

    public static class StrategyRegistry {
        private final Map<String, FeeCalculator> registry =
                Map.of(
                        "CREDIT_CARD", amount -> amount * 0.029 + 0.30,
                        "PAYPAL", amount -> amount * 0.034 + 0.35,
                        "DEBIT", amount -> 0.25);

        public double calculateFee(String paymentType, double amount) {
            FeeCalculator calculator = registry.get(paymentType);
            if (calculator == null) {
                throw new UnsupportedOperationException("Unknown payment type: " + paymentType);
            }
            return calculator.calculate(amount);
        }
    }

    public static void main(String[] args) {
        StrategyRegistry registry = new StrategyRegistry();
        double fee = registry.calculateFee("CREDIT_CARD", 100.0); // 3.20

        boolean calculated = Math.abs(fee - 3.20) < 0.001; // true

        System.out.println("Q17 fee: " + fee + ", calculated: " + calculated);
    }
}
