package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q18ThreadLocalPollutionExample {
    private Q18ThreadLocalPollutionExample() {}

    private static final ThreadLocal<String> USER_HOLDER = new ThreadLocal<>();

    public static void main(String[] args) {
        // Correct pattern: try-finally or AutoCloseable scope guarantees cleanup on pooled threads
        USER_HOLDER.set("user-tenant-alpha");
        try {
            String current = USER_HOLDER.get(); // "user-tenant-alpha"
        } finally {
            USER_HOLDER.remove(); // removes entry from pooled worker thread's ThreadLocalMap
        }

        String postCleanup =
                USER_HOLDER.get(); // null (next request on same thread cannot inherit contaminated
        // state)
    }
}
