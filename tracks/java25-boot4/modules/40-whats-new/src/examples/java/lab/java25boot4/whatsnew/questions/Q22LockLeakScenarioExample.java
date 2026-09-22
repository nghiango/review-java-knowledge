package lab.java25boot4.whatsnew.questions;

import java.util.concurrent.locks.ReentrantLock;

/** Q22: after a Java 25 upgrade a subsystem hangs; threads are parked on one lock. Why? */
public class Q22LockLeakScenarioExample {

    private final ReentrantLock lock = new ReentrantLock();
    private long sequence = 1000L;

    long nextSequence() {
        lock.lock();
        if (sequence >= 1000L) {
            // The designed terminal state throws while the lock is held and never releases it.
            throw new IllegalStateException("sequence exhausted");
        }
        try {
            return ++sequence;
        } finally {
            lock.unlock(); // present here, but absent in the broken upgrade PR
        }
    }

    public static void main(String[] args) {
        Q22LockLeakScenarioExample example = new Q22LockLeakScenarioExample();
        try {
            example.nextSequence();
        } catch (IllegalStateException exhausted) {
            System.out.println(exhausted.getMessage()); // sequence exhausted
        }
        System.out.println(example.lock.isLocked()); // false — the finally released the lock
    }
}
