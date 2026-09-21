package lab.architecture.questions;

import java.math.BigDecimal;

/**
 * Q22: Scenario: Refactoring a 5,000-line God Service into Hexagonal DDD Aggregates. Demonstrates
 * moving procedural price calculation and invariant logic from a service into an aggregate.
 */
public class Q22GodServiceRefactoringScenarioExample {

    public static class RefactoredOrderAggregate {
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal discountRate = BigDecimal.ZERO;

        public void applyCoupon(String couponCode) {
            if ("SAVE10".equals(couponCode)) {
                this.discountRate = new BigDecimal("0.10");
            }
        }

        public void addLineItem(BigDecimal price) {
            this.subtotal = this.subtotal.add(price);
        }

        public BigDecimal calculateFinalTotal() {
            BigDecimal discountAmount = this.subtotal.multiply(this.discountRate);
            return this.subtotal.subtract(discountAmount);
        }
    }

    public static void main(String[] args) {
        RefactoredOrderAggregate order = new RefactoredOrderAggregate();
        order.addLineItem(new BigDecimal("100.00"));
        order.applyCoupon("SAVE10");

        BigDecimal total =
                order.calculateFinalTotal(); // 90.00 (encapsulated in aggregate, not in god
        // service)
        boolean isCorrect = total.compareTo(new BigDecimal("90.00")) == 0; // true

        System.out.println("Q22 total: " + total + ", valid: " + isCorrect);
    }
}
