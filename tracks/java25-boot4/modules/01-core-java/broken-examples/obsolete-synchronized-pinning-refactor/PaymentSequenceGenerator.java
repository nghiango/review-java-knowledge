package lab.java25boot4.corejava.broken.pinning;

import java.util.concurrent.locks.ReentrantLock;

public class PaymentSequenceGenerator {

    private final ReentrantLock lock = new ReentrantLock();
    private long currentSequence = 1000L;

    public long nextSequence() {
        lock.lock();
        if (currentSequence >= 999999L) {
            throw new IllegalStateException("Sequence exhausted");
        }
        long next = ++currentSequence;
        lock.unlock();
        return next;
    }
}
