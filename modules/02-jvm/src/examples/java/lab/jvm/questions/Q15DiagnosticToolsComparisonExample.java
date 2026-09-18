package lab.jvm.questions;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

@SuppressWarnings("unused")
public final class Q15DiagnosticToolsComparisonExample {
    private Q15DiagnosticToolsComparisonExample() {}

    public static void main(String[] args) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int liveThreads =
                threadBean.getThreadCount(); // e.g. 5 (thread dumps inspect thread states & locks)

        long freeHeap =
                Runtime.getRuntime()
                        .freeMemory(); // e.g. 250000000 (heap dumps inspect retained graphs)
    }
}
