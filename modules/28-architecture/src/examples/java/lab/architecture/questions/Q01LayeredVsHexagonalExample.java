package lab.architecture.questions;

import java.math.BigDecimal;

/**
 * Q01: Layered vs Hexagonal Architecture (Dependency Direction). Demonstrates how Hexagonal
 * architecture inverts dependency so domain does not depend on database.
 */
public class Q01LayeredVsHexagonalExample {

    // Domain Port (Interface defined by domain)
    public interface OrderRepositoryPort {
        void save(String orderId, BigDecimal amount);
    }

    // Domain Model
    public static class OrderDomain {
        public static boolean isEligibleForDiscount(BigDecimal amount) {
            return amount.compareTo(new BigDecimal("100.00")) >= 0;
        }
    }

    // Infrastructure Adapter (implements port)
    public static class PostgresOrderAdapter implements OrderRepositoryPort {
        @Override
        public void save(String orderId, BigDecimal amount) {
            // Persists to DB
        }
    }

    public static void main(String[] args) {
        boolean eligible = OrderDomain.isEligibleForDiscount(new BigDecimal("150.00")); // true
        boolean notEligible = OrderDomain.isEligibleForDiscount(new BigDecimal("50.00")); // false

        System.out.println("Q01 eligible: " + eligible + ", notEligible: " + notEligible);
    }
}
