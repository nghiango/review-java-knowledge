package lab.observability.questions;

import org.slf4j.MDC;

public class Q02Slf4jMdcBasicsExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // SLF4J MDC (Mapped Diagnostic Context) stores key-value pairs in ThreadLocal storage.
        // Logging frameworks (Logback) append MDC values to every log statement emitted on that
        // thread.
        MDC.put("traceId", "tr-98765");
        MDC.put("userId", "usr-42");

        String retrievedTrace = MDC.get("traceId"); // "tr-98765"
        boolean containsUser = MDC.get("userId") != null; // true

        // Crucial invariant: Always clear MDC when the operation finishes to avoid thread reuse
        // pollution
        MDC.clear();
        String afterClear = MDC.get("traceId"); // null

        System.out.println("Trace ID stored in MDC: " + retrievedTrace);
        System.out.println("Trace ID after clear: " + afterClear);
    }
}
