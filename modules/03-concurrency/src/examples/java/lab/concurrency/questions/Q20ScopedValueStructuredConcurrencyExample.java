package lab.concurrency.questions;

/** Q20: Demonstrates ThreadLocal contextual scoping and unbinding lifecycle. */
@SuppressWarnings("unused")
public class Q20ScopedValueStructuredConcurrencyExample {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    public static void main(String[] args) {
        // ThreadLocal: per-thread storage requiring explicit remove() in pooled environments
        CORRELATION_ID.set("req-trace-12345");
        String currentTrace = CORRELATION_ID.get(); // "req-trace-12345"

        try {
            boolean isBound = CORRELATION_ID.get() != null; // true
        } finally {
            CORRELATION_ID
                    .remove(); // Prevents memory leaks and context leakage across pooled tasks
        }

        String afterCleanup = CORRELATION_ID.get(); // null
    }
}
