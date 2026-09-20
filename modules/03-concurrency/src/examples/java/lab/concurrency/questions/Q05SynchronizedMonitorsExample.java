package lab.concurrency.questions;

/** Q05: Demonstrates synchronized method vs block and intrinsic object monitor locking. */
@SuppressWarnings("unused")
public class Q05SynchronizedMonitorsExample {

    private final Object lock = new Object();
    private int balance = 100;

    // Instance method synchronization locks 'this'
    public synchronized void deposit(int amount) {
        this.balance += amount;
    }

    // Explicit block synchronization locks private monitor object
    public void withdraw(int amount) {
        synchronized (lock) {
            this.balance -= amount;
        }
    }

    public static void main(String[] args) {
        Q05SynchronizedMonitorsExample example = new Q05SynchronizedMonitorsExample();
        example.deposit(50);
        int balanceAfterDeposit = example.balance; // 150
        example.withdraw(30);
        int balanceAfterWithdraw = example.balance; // 120
    }
}
