package lab.java25boot4.concurrency.questions;

public class Q08VirtualThreadJfrPinningMetricsExample {

    public static void main(String[] args) {
        // System property -Djdk.tracePinnedThreads=full is the primary flag to diagnose pinning
        String traceProperty = System.getProperty("jdk.tracePinnedThreads", "short");

        System.out.println(
                "Trace pinned threads setting: "
                        + traceProperty); // Trace pinned threads setting: short
        System.out.println(
                "Synchronized blocks unpinned in Java 25: true"); // Synchronized blocks unpinned in
        // Java 25: true
        System.out.println(
                "JNI native frames still pin carrier threads: true"); // JNI native frames still pin
        // carrier threads: true
    }
}
