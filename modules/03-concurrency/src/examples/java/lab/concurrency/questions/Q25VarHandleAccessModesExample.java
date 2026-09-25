package lab.concurrency.questions;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("unused")
public final class Q25VarHandleAccessModesExample {
    private Q25VarHandleAccessModesExample() {}

    public static class CounterHolder {
        public volatile int count = 0;
    }

    private static final VarHandle COUNT_HANDLE;

    static {
        try {
            COUNT_HANDLE = MethodHandles.lookup().findVarHandle(CounterHolder.class, "count", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) {
        CounterHolder holder = new CounterHolder();

        // 1. Plain get/set: No memory fences, standard non-volatile read/write
        COUNT_HANDLE.set(holder, 10);
        int plainVal = (int) COUNT_HANDLE.get(holder); // 10

        // 2. Release/Acquire: One-way memory fence.
        // setRelease prevents memory writes from reordering AFTER the store;
        // getAcquire prevents memory reads from reordering BEFORE the load.
        COUNT_HANDLE.setRelease(holder, 20);
        int acquireVal = (int) COUNT_HANDLE.getAcquire(holder); // 20

        // 3. Volatile / CAS: Full bidirectional hardware memory barrier
        boolean updated = COUNT_HANDLE.compareAndSet(holder, 20, 30); // true
        int finalVal = (int) COUNT_HANDLE.getVolatile(holder); // 30
    }
}
