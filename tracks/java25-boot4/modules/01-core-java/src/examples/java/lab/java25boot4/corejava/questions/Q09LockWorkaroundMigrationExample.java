package lab.java25boot4.corejava.questions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Q09LockWorkaroundMigrationExample {

    // Java 21 workaround: hand-written lock because synchronized "pinned" carrier threads.
    static final class ReentrantLockedCounter {
        private final ReentrantLock lock = new ReentrantLock();
        private long value = 1000L;

        long next() {
            lock.lock();
            try {
                return ++value;
            } finally {
                lock.unlock();
            }
        }
    }

    // Java 25 delta: synchronized no longer pins, so the keyword is enough again.
    static final class SynchronizedCounter {
        private long value = 1000L;

        synchronized long next() {
            return ++value;
        }
    }

    // For a plain monotonic counter, drop locking altogether.
    static final class AtomicCounter {
        private final AtomicLong value = new AtomicLong(1000L);

        long next() {
            return value.incrementAndGet();
        }
    }

    public static void main(String[] args) {
        System.out.println(new ReentrantLockedCounter().next()); // 1001
        System.out.println(new SynchronizedCounter().next()); // 1001
        System.out.println(new AtomicCounter().next()); // 1001
    }
}
