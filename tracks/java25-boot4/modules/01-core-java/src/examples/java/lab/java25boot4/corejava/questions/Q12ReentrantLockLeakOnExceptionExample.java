package lab.java25boot4.corejava.questions;

import java.util.concurrent.locks.ReentrantLock;

public class Q12ReentrantLockLeakOnExceptionExample {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();

        try {
            lock.lock();
            throw new IllegalStateException("sequence exhausted");
        } catch (IllegalStateException e) {
            // No finally block: the lock is never released on the exception path.
        }

        System.out.println(lock.isLocked()); // true
        System.out.println(lock.tryLock()); // false — every later caller blocks forever
    }
}
