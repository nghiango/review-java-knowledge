package lab.java25boot4.concurrency.questions;

public class Q09MigrationThreadLocalToScopedValueExample {

    // Migration from ThreadLocal to ScopedValue
    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    public static void main(String[] args) {
        // Old:
        // ThreadLocal<String> tl = new ThreadLocal<>();
        // try { tl.set("tenant-1"); run(); } finally { tl.remove(); }

        // New in Java 25:
        ScopedValue.where(TENANT_ID, "tenant-1")
                .run(
                        () -> {
                            System.out.println(
                                    "Processing for: "
                                            + TENANT_ID.get()); // Processing for: tenant-1
                        });

        System.out.println(
                "Cleaned up automatically: "
                        + (!TENANT_ID.isBound())); // Cleaned up automatically: true
    }
}
