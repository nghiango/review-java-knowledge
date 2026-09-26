package lab.performance.questions;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates an incident where continuous dynamic proxy/bytecode generation (e.g. CGLIB, Groovy, or un-cached
 * reflection proxies) leaked ClassLoader references, exhausting JVM Metaspace and crashing with OutOfMemoryError.
 */
public final class Q29IncidentMetaspaceLeakProxyGenerationExample {
    public static void main(String[] args) {
        long metaspaceMaxBytes = 256L * 1024 * 1024; // 256MB
        long metaspaceUsedBytes = 250L * 1024 * 1024;

        // A burst of un-cached dynamic proxies pushes metaspace over the limit
        metaspaceUsedBytes += 10L * 1024 * 1024;

        boolean isMetaspaceOom = metaspaceUsedBytes > metaspaceMaxBytes; // true
        System.out.println("Metaspace capacity exceeded: " + isMetaspaceOom);
    }
}
