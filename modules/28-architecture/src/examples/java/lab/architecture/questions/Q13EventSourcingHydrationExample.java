package lab.architecture.questions;

import java.util.List;

/**
 * Q13: Event Sourcing Concepts (Event Streams & State Hydration). Demonstrates rehydrating state
 * from an append-only sequence of past events.
 */
public class Q13EventSourcingHydrationExample {

    public interface DomainEvent {}

    public record AccountOpenedEvent(String accountId, int initialBalance) implements DomainEvent {}

    public record MoneyDepositedEvent(String accountId, int amount) implements DomainEvent {}

    public record MoneyWithdrawnEvent(String accountId, int amount) implements DomainEvent {}

    public static class BankAccountAggregate {
        private String accountId;
        private int balance = 0;

        // Rehydrates state by replaying past events in order
        public static BankAccountAggregate replay(List<DomainEvent> eventStream) {
            BankAccountAggregate aggregate = new BankAccountAggregate();
            for (DomainEvent event : eventStream) {
                aggregate.apply(event);
            }
            return aggregate;
        }

        private void apply(DomainEvent event) {
            if (event instanceof AccountOpenedEvent e) {
                this.accountId = e.accountId();
                this.balance = e.initialBalance();
            } else if (event instanceof MoneyDepositedEvent e) {
                this.balance += e.amount();
            } else if (event instanceof MoneyWithdrawnEvent e) {
                this.balance -= e.amount();
            }
        }

        public int getBalance() {
            return balance;
        }
    }

    public static void main(String[] args) {
        List<DomainEvent> history =
                List.of(
                        new AccountOpenedEvent("ACC-1", 100),
                        new MoneyDepositedEvent("ACC-1", 50),
                        new MoneyWithdrawnEvent("ACC-1", 30));

        BankAccountAggregate account = BankAccountAggregate.replay(history);
        int finalBalance = account.getBalance(); // 120 (100 + 50 - 30)
        boolean isCorrect = (finalBalance == 120); // true

        System.out.println("Q13 finalBalance: " + finalBalance + ", isCorrect: " + isCorrect);
    }
}
