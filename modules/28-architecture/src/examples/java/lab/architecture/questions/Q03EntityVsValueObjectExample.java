package lab.architecture.questions;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Q03: Entity vs Value Object in Domain-Driven Design. Demonstrates identity comparison (Entity) vs
 * structural equality (Value Object).
 */
public class Q03EntityVsValueObjectExample {

    // Value Object: Defined solely by attributes, immutable, no unique identity
    public record Money(BigDecimal amount, String currency) {
        public Money {
            Objects.requireNonNull(amount);
            Objects.requireNonNull(currency);
        }
    }

    // Entity: Defined by persistent thread of identity, mutable across lifecycle
    public static class CustomerEntity {
        private final String id;
        private String email;

        public CustomerEntity(String id, String email) {
            this.id = id;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static void main(String[] args) {
        Money m1 = new Money(new BigDecimal("10.00"), "USD");
        Money m2 = new Money(new BigDecimal("10.00"), "USD");

        boolean valueObjectEquals = m1.equals(m2); // true (value equality)
        boolean sameReference = (m1 == m2); // false (different objects)

        CustomerEntity c1 = new CustomerEntity("CUST-1", "user@example.com");
        CustomerEntity c2 = new CustomerEntity("CUST-1", "user-updated@example.com");
        boolean sameIdentity =
                c1.getId().equals(c2.getId()); // true (same entity identity despite changed
        // attribute)

        System.out.println("Q03 voEquals: " + valueObjectEquals + ", sameId: " + sameIdentity);
    }
}
