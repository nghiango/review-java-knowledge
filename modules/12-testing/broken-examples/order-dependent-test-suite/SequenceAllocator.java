package lab.testing.broken.orderdependenttestsuite;

/**
 * Issues monotonically increasing sequence numbers.
 *
 * <p>{@code start} is the value the counter holds before the first allocation, so {@code new
 * SequenceAllocator(0).next()} returns 1 and {@link #current()} returns 0 until then. Allocation
 * stops at {@link Long#MAX_VALUE} with an {@link ArithmeticException} instead of wrapping to a
 * negative value.
 */
public final class SequenceAllocator {

    private static long counter;

    private final long start;

    public SequenceAllocator(long start) {
        this.start = start;
    }

    /**
     * Advances the counter and returns the newly allocated value.
     *
     * @throws ArithmeticException if the counter has already reached {@link Long#MAX_VALUE}
     */
    public long next() {
        counter = Math.incrementExact(counter);
        return counter;
    }

    /** Returns the last allocated value, or {@code start} if nothing has been allocated yet. */
    public long current() {
        return counter;
    }

    /** Restores the counter to {@code start}, so the next allocation is {@code start + 1}. */
    public void reset() {
        counter = start;
    }
}
