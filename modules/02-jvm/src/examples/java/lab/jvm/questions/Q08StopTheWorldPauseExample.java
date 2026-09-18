package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q08StopTheWorldPauseExample {
    private Q08StopTheWorldPauseExample() {}

    public static void main(String[] args) {
        // At safepoints (e.g. GC phase or heap dump), JIT safepoint poll instructions halt all
        // threads
        long start = System.nanoTime();
        byte[] memoryBlock = new byte[1024 * 1024]; // 1 MB allocated
        long elapsed = System.nanoTime() - start; // threads run uninterrupted outside safepoints
    }
}
