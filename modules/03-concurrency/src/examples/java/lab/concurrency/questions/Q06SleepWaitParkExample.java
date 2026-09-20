package lab.concurrency.questions;

import java.util.concurrent.locks.LockSupport;

/** Q06: Demonstrates Thread.sleep() vs Object.wait() vs LockSupport.park(). */
@SuppressWarnings("unused")
public class Q06SleepWaitParkExample {

    public static void main(String[] args) throws InterruptedException {
        // 1. Thread.sleep() keeps acquired locks while transitioning to TIMED_WAITING
        long start = System.currentTimeMillis();
        Thread.sleep(10);
        long elapsed = System.currentTimeMillis() - start; // >= 10 ms

        // 2. LockSupport.park() / unpark() provides permit-based parking without monitor ownership
        Thread unparkedThread = Thread.currentThread();
        LockSupport.unpark(unparkedThread); // Give permit in advance
        LockSupport.park(); // Consumes permit immediately without blocking
        boolean stillRunning = true; // true
    }
}
