package lab.concurrency.questions;

/** Q04: Demonstrates volatile visibility guarantees and compound action limits. */
@SuppressWarnings("unused")
public class Q04VolatileGuaranteesExample {

    private volatile boolean ready = false;
    private int data = 0;

    public void writer() {
        data = 42; // Action before volatile write
        ready = true; // Volatile write establishes happens-before with volatile read
    }

    public int reader() {
        if (ready) { // Volatile read
            return data; // Guaranteed 42 by JMM happens-before transitive visibility
        }
        return -1;
    }

    public static void main(String[] args) {
        Q04VolatileGuaranteesExample example = new Q04VolatileGuaranteesExample();
        example.writer();
        int observed = example.reader(); // 42
    }
}
