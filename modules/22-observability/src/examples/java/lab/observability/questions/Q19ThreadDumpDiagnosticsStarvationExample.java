package lab.observability.questions;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class Q19ThreadDumpDiagnosticsStarvationExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Diagnosing thread starvation with ThreadMXBean:
        // Captures thread states (RUNNABLE, WAITING, TIMED_WAITING, BLOCKED) and lock ownership.
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = mxBean.dumpAllThreads(true, true);

        int totalThreads = threadInfos.length;
        long[] deadlockedThreads = mxBean.findDeadlockedThreads();

        boolean hasDeadlocks = deadlockedThreads != null && deadlockedThreads.length > 0; // false
        boolean threadsCaptured = totalThreads > 0; // true

        System.out.println("JVM live threads inspected: " + totalThreads);
        System.out.println("Deadlocks detected: " + hasDeadlocks);
    }
}
