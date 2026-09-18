package lab.jvm.questions;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public final class Q16OutOfMemoryTaxonomyExample {
    private Q16OutOfMemoryTaxonomyExample() {}

    public static void main(String[] args) {
        // Direct byte buffers allocate off-heap native memory (subject to -XX:MaxDirectMemorySize)
        ByteBuffer directBuffer =
                ByteBuffer.allocateDirect(1024 * 1024); // 1 MB direct off-heap buffer
        boolean isDirect =
                directBuffer.isDirect(); // true (off-heap native memory outside standard Java heap)
    }
}
