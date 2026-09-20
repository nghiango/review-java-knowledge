package lab.concurrency.questions;

import java.util.concurrent.locks.ReentrantLock;

/** Q19: Demonstrates Java 21 Virtual Threads and avoiding carrier pinning with ReentrantLock. */
@SuppressWarnings("unused")
public class Q19VirtualThreadPinningExample {

    private static final ReentrantLock unpinningLock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        // Virtual thread builder in Java 21
        Thread vThread =
                Thread.ofVirtual()
                        .name("vt-worker")
                        .start(
                                () -> {
                                    boolean isVirtual = Thread.currentThread().isVirtual(); // true

                                    // ReentrantLock yields carrier thread when unparked/parked
                                    // without pinning
                                    unpinningLock.lock();
                                    try {
                                        // Non-pinning blocking execution
                                    } finally {
                                        unpinningLock.unlock();
                                    }
                                });

        vThread.join();
        boolean finished = !vThread.isAlive(); // true
    }
}
