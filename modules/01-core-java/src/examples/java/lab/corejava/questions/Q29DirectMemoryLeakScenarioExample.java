package lab.corejava.questions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q29DirectMemoryLeakScenarioExample {
    private Q29DirectMemoryLeakScenarioExample() {}

    public static void main(String[] args) {
        // DirectByteBuffer allocates memory outside the JVM garbage-collected heap via malloc.
        // It relies on java.lang.ref.Cleaner (a PhantomReference) to invoke unsafe.freeMemory()
        // only when the small direct buffer heap wrapper is garbage collected.
        List<ByteBuffer> retainedBuffers = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            // Allocates 10 MB off-heap per iteration
            ByteBuffer directBuf = ByteBuffer.allocateDirect(10 * 1024 * 1024);
            directBuf.putInt(42);
            directBuf.flip();

            int val = directBuf.getInt(); // 42
            retainedBuffers.add(directBuf);
        }

        // Retaining DirectByteBuffer references retains the off-heap native memory.
        // Because heap usage remains minuscule (a few wrapper objects), standard heap GC
        // is never triggered, eventually causing java.lang.OutOfMemoryError: Direct buffer memory
        // or silent process termination by OS Linux OOMKiller.
        boolean isDirect = retainedBuffers.getFirst().isDirect(); // true
    }
}
