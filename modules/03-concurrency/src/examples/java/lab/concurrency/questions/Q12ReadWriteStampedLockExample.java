package lab.concurrency.questions;

import java.util.concurrent.locks.StampedLock;

/** Q12: Demonstrates StampedLock optimistic reading and validate(). */
@SuppressWarnings("unused")
public class Q12ReadWriteStampedLockExample {

    private final StampedLock stampedLock = new StampedLock();
    private int x = 10;
    private int y = 20;

    public int optimisticReadSum() {
        // Optimistic read acquires no mutual-exclusion lock, returning a stamp
        long stamp = stampedLock.tryOptimisticRead();
        int currentX = x;
        int currentY = y;

        // Check if an intervening write occurred
        if (!stampedLock.validate(stamp)) {
            // Fall back to pessimistic read lock
            stamp = stampedLock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
        return currentX + currentY;
    }

    public static void main(String[] args) {
        Q12ReadWriteStampedLockExample example = new Q12ReadWriteStampedLockExample();
        int sum = example.optimisticReadSum(); // 30
    }
}
