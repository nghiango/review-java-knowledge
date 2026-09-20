package lab.concurrency.questions;

/** Q03: Demonstrates race condition vs data race vs atomicity. */
@SuppressWarnings("unused")
public class Q03RaceConditionVsDataRaceExample {

    private int unsafeCounter = 0;

    public void incrementUnsafe() {
        // Data race + lost update race condition: read, add, write
        unsafeCounter++; // 3 bytecode ops: GETFIELD, IADD, PUTFIELD
    }

    public static void main(String[] args) {
        Q03RaceConditionVsDataRaceExample example = new Q03RaceConditionVsDataRaceExample();
        example.incrementUnsafe();
        int result = example.unsafeCounter; // 1 (in single thread, but non-deterministic under
        // concurrent threads)
    }
}
