package lab.java25boot4.whatsnew.broken.unsafebuffer;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * Off-heap buffer for the metric-ingestion hot path. The intent is to keep high-volume samples out
 * of the Java heap so that they never contribute to GC pressure.
 */
public class UnsafeOffHeapBuffer {

    private static final Unsafe UNSAFE = loadUnsafe();

    private final long address;
    private final int capacity;

    public UnsafeOffHeapBuffer(int capacity) {
        this.capacity = capacity;
        this.address = UNSAFE.allocateMemory((long) capacity * Long.BYTES);
    }

    public void set(int index, long value) {
        UNSAFE.putLong(address + (long) index * Long.BYTES, value);
    }

    public long get(int index) {
        return UNSAFE.getLong(address + (long) index * Long.BYTES);
    }

    public int capacity() {
        return capacity;
    }

    public void close() {
        UNSAFE.freeMemory(address);
    }

    private static Unsafe loadUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available", e);
        }
    }
}
