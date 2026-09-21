package lab.designpatterns.questions;

import java.util.function.Predicate;

/**
 * Q09: Specification Pattern Composition. Demonstrates combining domain rules with AND, OR, and NOT
 * predicates.
 */
public class Q09SpecificationPatternCompositionExample {

    public record Customer(int age, boolean hasActiveSubscription) {}

    public static class Specification<T> {
        private final Predicate<T> predicate;

        public Specification(Predicate<T> predicate) {
            this.predicate = predicate;
        }

        public boolean isSatisfiedBy(T candidate) {
            return predicate.test(candidate);
        }

        public Specification<T> and(Specification<T> other) {
            return new Specification<>(this.predicate.and(other.predicate));
        }

        public Specification<T> or(Specification<T> other) {
            return new Specification<>(this.predicate.or(other.predicate));
        }
    }

    public static void main(String[] args) {
        Specification<Customer> isAdult = new Specification<>(c -> c.age() >= 18);
        Specification<Customer> isSubscribed = new Specification<>(Customer::hasActiveSubscription);

        Specification<Customer> eligibleForDiscount = isAdult.and(isSubscribed);

        Customer c1 = new Customer(25, true);
        Customer c2 = new Customer(16, true);

        boolean c1Eligible = eligibleForDiscount.isSatisfiedBy(c1); // true
        boolean c2Eligible = eligibleForDiscount.isSatisfiedBy(c2); // false

        System.out.println("Q09 c1: " + c1Eligible + ", c2: " + c2Eligible);
    }
}
