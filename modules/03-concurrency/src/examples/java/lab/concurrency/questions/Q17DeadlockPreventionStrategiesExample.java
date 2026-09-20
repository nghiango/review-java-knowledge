package lab.concurrency.questions;

import java.util.concurrent.locks.ReentrantLock;

/** Q17: Demonstrates deadlock prevention through deterministic lock ordering. */
@SuppressWarnings({"unused", "ReferenceEquality"})
public class Q17DeadlockPreventionStrategiesExample {

    private final ReentrantLock lockA = new ReentrantLock();
    private final ReentrantLock lockB = new ReentrantLock();

    public boolean transferFunds(ReentrantLock first, ReentrantLock second) {
        // Enforce deterministic global ordering by identityHashCode
        ReentrantLock primary =
                System.identityHashCode(first) < System.identityHashCode(second) ? first : second;
        ReentrantLock secondary = primary == first ? second : first;

        primary.lock();
        try {
            secondary.lock();
            try {
                return true; // Mutual exclusion achieved without circular wait
            } finally {
                secondary.unlock();
            }
        } finally {
            primary.unlock();
        }
    }

    public static void main(String[] args) {
        Q17DeadlockPreventionStrategiesExample example =
                new Q17DeadlockPreventionStrategiesExample();
        boolean success = example.transferFunds(example.lockA, example.lockB); // true
    }
}
