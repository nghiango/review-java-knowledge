package lab.java25boot4.springsecurity.questions;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Q10: How do you migrate legacy InheritableThreadLocal security context propagation to Java 25
 * Scoped Values?
 */
public class Q10MigrationInheritableThreadLocalToScopedValueExample {

    public static final ScopedValue<String> TENANT_AUTH = ScopedValue.newInstance();

    public static void main(String[] args) {
        AtomicReference<String> tenantCaptured = new AtomicReference<>();

        ScopedValue.where(TENANT_AUTH, "tenant-corp-alpha")
                .run(
                        () -> {
                            tenantCaptured.set(TENANT_AUTH.get());
                        });

        System.out.println("Captured tenant: " + tenantCaptured.get()); // "tenant-corp-alpha"
        System.out.println("Cleaned up after scope: " + !TENANT_AUTH.isBound()); // true
    }
}
