package lab.java25boot4.whatsnew.questions;

import java.util.concurrent.locks.ReentrantLock;

/** Q20: a teammate keeps every {@code ReentrantLock} rewrite "to be safe" — what do you check? */
public class Q20LockRevertDecisionExample {

    private final ReentrantLock lock = new ReentrantLock();
    private long sequence = 1000L;

    // A lock is only worth keeping when it needs tryLock, fairness or multiple Conditions.
    boolean needsReentrantLock() {
        return lock.isFair() || lock.getQueueLength() >= 0 && lock.tryLock();
    }

    public static void main(String[] args) {
        Q20LockRevertDecisionExample example = new Q20LockRevertDecisionExample();
        System.out.println(example.needsReentrantLock()); // false — a plain counter needs no lock
        System.out.println(example.sequence); // 1000 — synchronized or AtomicLong is the safer choice
    }
}
