package lab.architecture.questions;

import java.math.BigDecimal;

/**
 * Q02: Anemic Domain Model vs Rich Domain Model. Demonstrates how rich domain model enforces
 * business invariants internally.
 */
public class Q02AnemicVsRichDomainExample {

    // Rich Domain Model: State and behavior encapsulated
    public static class BankAccount {
        private BigDecimal balance;

        public BankAccount(BigDecimal initialBalance) {
            if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Negative initial balance");
            }
            this.balance = initialBalance;
        }

        public void withdraw(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be positive");
            }
            if (this.balance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds");
            }
            this.balance = this.balance.subtract(amount);
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }

    public static void main(String[] args) {
        BankAccount account = new BankAccount(new BigDecimal("100.00"));
        account.withdraw(new BigDecimal("40.00"));

        BigDecimal remaining = account.getBalance(); // 60.00 (invariant protected)
        boolean hasSixty = remaining.compareTo(new BigDecimal("60.00")) == 0; // true

        System.out.println("Q02 remaining: " + remaining + ", valid: " + hasSixty);
    }
}
