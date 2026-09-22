package lab.java25boot4.whatsnew.unsafebuffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Off-heap {@code long} buffer built on the Foreign Function &amp; Memory API (final in Java 22).
 *
 * <p>Decision: an {@link Arena} owns the native memory instead of a raw {@code Unsafe} address. The
 * arena makes the lifetime structural — closing it frees everything it allocated — and
 * {@link MemorySegment} bounds-checks every access, so a bad index fails as a normal Java exception
 * rather than corrupting native memory.
 *
 * <p>Trade-off: the buffer is confined to the thread that created it ({@link Arena#ofConfined()}).
 * Use {@link Arena#ofShared()} if the buffer must be shared across threads, at a small
 * synchronisation cost.
 */
public final class ArenaMemoryBuffer implements AutoCloseable {

    private final Arena arena;
    private final MemorySegment segment;
    private final int capacity;

    public ArenaMemoryBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive but was " + capacity);
        }
        this.capacity = capacity;
        this.arena = Arena.ofConfined();
        this.segment = arena.allocate((long) capacity * Long.BYTES);
    }

    public void set(int index, long value) {
        segment.set(ValueLayout.JAVA_LONG, offset(index), value);
    }

    public long get(int index) {
        return segment.get(ValueLayout.JAVA_LONG, offset(index));
    }

    public int capacity() {
        return capacity;
    }

    private long offset(int index) {
        if (index < 0 || index >= capacity) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " is outside the buffer range [0, " + capacity + ")");
        }
        return (long) index * Long.BYTES;
    }

    @Override
    public void close() {
        arena.close();
    }
}
