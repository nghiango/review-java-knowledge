package lab.testing.sequence;

/**
 * Issues monotonically increasing sequence numbers from a counter owned by each instance.
 *
 * <p>The counter is instance state, not a static field: two allocators created with the same start
 * value advance independently, and nothing one instance does is observable by another. That is what
 * lets a caller hold several sequences in one process — one per tenant, per partition, per stream —
 * and what lets a test construct an allocator per test instead of sharing one.
 *
 * <p>{@code start} is the value the counter holds before the first allocation, so {@code new
 * SequenceAllocator(0).next()} returns 1 and {@link #current()} returns 0 until then. Allocation
 * stops at {@link Long#MAX_VALUE} with an {@link ArithmeticException} instead of wrapping to a
 * negative value: a silently wrapped sequence hands out numbers that were already issued, which is
 * worse for a caller than an explicit failure it can handle.
 */
public final class SequenceAllocator {

    private final long start;
    private long counter;

    public SequenceAllocator(long start) {
        this.start = start;
        this.counter = start;
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
