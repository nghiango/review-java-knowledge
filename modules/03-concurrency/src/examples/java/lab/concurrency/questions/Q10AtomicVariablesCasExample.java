package lab.concurrency.questions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/** Q10: Demonstrates compare-and-swap (CAS) and ABA prevention with AtomicStampedReference. */
@SuppressWarnings({"unused", "UnnecessaryAsync"})
public class Q10AtomicVariablesCasExample {

    public static void main(String[] args) {
        AtomicInteger atomicInt = new AtomicInteger(10);
        boolean casSuccess = atomicInt.compareAndSet(10, 20); // true
        boolean casFail = atomicInt.compareAndSet(10, 30); // false (value is now 20)

        // ABA prevention: tracking value + stamp integer
        String initialRef = "A";
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>(initialRef, 1);

        int stamp = stampedRef.getStamp(); // 1
        boolean updatedToB = stampedRef.compareAndSet("A", "B", stamp, stamp + 1); // true
        boolean updatedBackToA = stampedRef.compareAndSet("B", "A", stamp + 1, stamp + 2); // true

        // Value is 'A', but stamp is 3 (original stamp 1 check fails, preventing ABA bug)
        boolean abaPrevented = stampedRef.compareAndSet("A", "C", 1, 4); // false (stamp mismatch!)
    }
}
