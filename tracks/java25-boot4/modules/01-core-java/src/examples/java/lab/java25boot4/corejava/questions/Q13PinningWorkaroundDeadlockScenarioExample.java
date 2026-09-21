package lab.java25boot4.corejava.questions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Q13PinningWorkaroundDeadlockScenarioExample {

    static final class SequenceGenerator {
        private final ReentrantLock lock = new ReentrantLock();
        private long sequence = 1000L;

        long next() {
            lock.lock();
            if (sequence >= 1001L) {
                throw new IllegalStateException("sequence exhausted");
            }
            long next = ++sequence;
            lock.unlock();
            return next;
        }

        boolean isLockHeld() {
            return lock.isLocked();
        }

        boolean tryAcquire(long timeoutMillis) throws InterruptedException {
            return lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SequenceGenerator generator = new SequenceGenerator();
        System.out.println(generator.next()); // 1001

        try {
            generator.next();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage()); // "sequence exhausted"
        }

        System.out.println(generator.isLockHeld()); // true — the lock leaked on the exception path
        System.out.println(generator.tryAcquire(100)); // false — production callers block forever
    }
}
