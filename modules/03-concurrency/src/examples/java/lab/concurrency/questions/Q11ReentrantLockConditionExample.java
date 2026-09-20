package lab.concurrency.questions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Q11: Demonstrates ReentrantLock, timed tryLock, and Condition variables. */
@SuppressWarnings("unused")
public class Q11ReentrantLockConditionExample {

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(true); // Fair ordering
        Condition condition = lock.newCondition();

        boolean acquired = lock.tryLock(50, TimeUnit.MILLISECONDS); // true
        try {
            int holdCount = lock.getHoldCount(); // 1
            boolean isLocked = lock.isLocked(); // true
        } finally {
            lock.unlock();
        }

        boolean stillLocked = lock.isLocked(); // false
    }
}
