package lab.java25boot4.whatsnew.questions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/** Q10: how does an {@code Arena} bound the lifetime of native memory? */
public class Q10ArenaLifetimeExample {

    public static void main(String[] args) {
        MemorySegment escaped;
        try (Arena arena = Arena.ofConfined()) {
            escaped = arena.allocate(8);
            escaped.set(ValueLayout.JAVA_LONG, 0, 5L);
            System.out.println(escaped.get(ValueLayout.JAVA_LONG, 0)); // 5
        }

        try {
            escaped.get(ValueLayout.JAVA_LONG, 0);
        } catch (IllegalStateException closed) {
            System.out.println("access after close rejected"); // IllegalStateException: Already closed
        }
    }
}
