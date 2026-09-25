package lab.jvm.questions;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

@SuppressWarnings("unused")
public final class Q24ClassDataSharingExample {
    private Q24ClassDataSharingExample() {}

    public static void main(String[] args) {
        // Class Data Sharing (AppCDS) pre-processes classes into a memory-mapped archive (.jsa).
        // At startup, the JVM maps metadata into memory read-only/read-write, eliminating
        // classfile parsing, verification, and bytecode linking phases.
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMx.getInputArguments();

        // Flag: -XX:ArchiveClassesAtExit=app-cds.jsa (creates dynamic archive)
        // Flag: -XX:SharedArchiveFile=app-cds.jsa (maps shared archive at launch)
        boolean isShared = false;
        for (String arg : jvmArgs) {
            if (arg.contains("SharedArchiveFile")) {
                isShared = true; // true when running with active CDS archive
                break;
            }
        }

        // Reduced startup time: ~30-50% faster class loading
        // Reduced memory footprint: shared read-only memory pages shared across multiple JVM processes
    }
}
