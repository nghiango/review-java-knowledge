package lab.java25boot4.whatsnew.questions;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** Q7: why do {@code sun.misc.Unsafe} memory methods now warn? */
public class Q07UnsafeMemoryDeprecationExample {

    public static void main(String[] args) throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);

        // JEP 498: these methods are deprecated for removal and emit warnings on Java 25.
        long address = unsafe.allocateMemory(8);
        unsafe.putLong(address, 99L);
        System.out.println(unsafe.getLong(address)); // 99 — still works, but is on the removal list
        unsafe.freeMemory(address);

        System.out.println("compile with --warning-mode all to see the removal warnings"); // printed
    }
}
