package lab.concurrency.questions;

import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public final class Q27VirtualThreadCarrierPinningExample {
    private Q27VirtualThreadCarrierPinningExample() {}

    private static final Object MONITOR = new Object();
    private static final ReentrantLock LOCK = new ReentrantLock();

    // Problematic: synchronized block pins the carrier platform thread when blocking on I/O.
    // The JVM cannot unmount the virtual thread from its carrier OS thread!
    public static void pinnedOperation() {
        synchronized (MONITOR) {
            // Blocking I/O or sleep inside synchronized pins the underlying carrier thread!
            boolean held = Thread.holdsLock(MONITOR); // true
        }
    }

    // Solution: ReentrantLock unmounts cleanly on park without pinning the carrier thread!
    public static void nonPinnedOperation() {
        LOCK.lock();
        try {
            // Virtual thread can yield and unmount from its carrier thread during blocking I/O!
            boolean isLocked = LOCK.isHeldByCurrentThread(); // true
        } finally {
            LOCK.unlock();
        }
    }

    public static void main(String[] args) throws Exception {
        Thread vt = Thread.ofVirtual().start(() -> {
            nonPinnedOperation();
        });
        vt.join();
        boolean finished = !vt.isAlive(); // true
    }
}
