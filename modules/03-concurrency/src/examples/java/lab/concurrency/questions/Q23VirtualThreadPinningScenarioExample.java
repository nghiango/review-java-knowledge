package lab.concurrency.questions;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Q23 Scenario: Diagnosing virtual thread carrier pinning and migrating from synchronized to
 * ReentrantLock.
 */
@SuppressWarnings("unused")
public class Q23VirtualThreadPinningScenarioExample {

    // BAD: synchronized pinned carrier thread during blocking network / file I/O in Java 21
    // GOOD: ReentrantLock unparks carrier thread smoothly
    private final ReentrantLock unpinnedLock = new ReentrantLock();

    public void processIoUnpinned() {
        unpinnedLock.lock();
        try {
            // Simulated blocking I/O operation
            boolean executed = true;
        } finally {
            unpinnedLock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Q23VirtualThreadPinningScenarioExample scenario =
                new Q23VirtualThreadPinningScenarioExample();

        Thread vt = Thread.ofVirtual().start(scenario::processIoUnpinned);
        vt.join();

        boolean done = !vt.isAlive(); // true
    }
}
