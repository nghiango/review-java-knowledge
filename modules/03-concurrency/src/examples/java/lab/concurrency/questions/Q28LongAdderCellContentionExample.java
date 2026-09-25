package lab.concurrency.questions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings({"unused", "UnnecessaryAsync"})
public final class Q28LongAdderCellContentionExample {
    private Q28LongAdderCellContentionExample() {}

    public static void main(String[] args) {
        // AtomicLong: single memory location updated via compare-and-swap (CAS).
        // Under 64+ concurrent threads, CAS retries spin heavily, burning CPU cycles in cache-line ping-pong.
        AtomicLong atomicCounter = new AtomicLong(0L);
        atomicCounter.incrementAndGet(); // CAS loop updating single variable

        // LongAdder: maintains a dynamically resized table of Cell objects.
        // Each thread hashes to a distinct Cell, eliminating cache-line bouncing and CAS retries.
        LongAdder adder = new LongAdder();
        adder.increment(); // Distributes writes across independent cache-line padded cells
        adder.add(41L);

        // sum() aggregates all cells only when an aggregate total is requested
        long total = adder.sum(); // 42L
        long atomicTotal = atomicCounter.get(); // 1L
    }
}
