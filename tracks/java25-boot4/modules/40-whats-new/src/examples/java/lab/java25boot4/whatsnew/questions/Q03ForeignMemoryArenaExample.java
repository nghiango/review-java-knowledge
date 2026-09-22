package lab.java25boot4.whatsnew.questions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/** Q3: what does the Foreign Function &amp; Memory API replace? */
public class Q03ForeignMemoryArenaExample {

    public static void main(String[] args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(2 * Long.BYTES);
            segment.set(ValueLayout.JAVA_LONG, 0, 11L);
            segment.set(ValueLayout.JAVA_LONG, Long.BYTES, 22L);

            System.out.println(segment.get(ValueLayout.JAVA_LONG, 0)); // 11
            System.out.println(segment.byteSize()); // 16
        }
        System.out.println("arena closed -> native memory freed"); // printed after try-with-resources
    }
}
