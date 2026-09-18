package lab.jvm.questions;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@SuppressWarnings("unused")
public final class Q20LowHeapOffHeapOomExample {
    private Q20LowHeapOffHeapOomExample() {}

    public static void main(String[] args) {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long heapUsed = mem.getHeapMemoryUsage().getUsed(); // e.g. 50 MB
        long nonHeapUsed =
                mem.getNonHeapMemoryUsage()
                        .getUsed(); // e.g. 180 MB (Metaspace + CodeCache native memory)

        // Off-heap native memory exhaustion can trigger OOM while heap occupancy remains low
        boolean lowHeap = (heapUsed < 100 * 1024 * 1024); // true
    }
}
