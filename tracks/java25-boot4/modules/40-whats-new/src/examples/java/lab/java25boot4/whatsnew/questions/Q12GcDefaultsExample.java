package lab.java25boot4.whatsnew.questions;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/** Q12: what changed for GC defaults on Java 25? */
public class Q12GcDefaultsExample {

    public static void main(String[] args) {
        // ZGC is generational by default since Java 23 (JEP 474), so -XX:+UseZGC no longer needs
        // -XX:+ZGenerational. G1 remains the default collector unless ZGC is selected.
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println(gc.getName()); // e.g. "G1 Young Generation" / "G1 Old Generation"
        }
        System.out.println("re-check -XX flags inherited from Java 21"); // printed after the collector names
    }
}
