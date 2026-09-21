package lab.java25boot4.concurrency;

import java.lang.ScopedValue.CallableOp;

/**
 * Demonstrates immutable, bounded context propagation using ScopedValue. Replaces ThreadLocal for
 * virtual thread workloads without manual cleanup.
 */
public class ScopedValueSecurityContext {

    public record UserPrincipal(String userId, String role) {}

    public static final ScopedValue<UserPrincipal> CURRENT_USER = ScopedValue.newInstance();

    public static <T, X extends Throwable> T runAs(UserPrincipal principal, CallableOp<T, X> action)
            throws X {
        return ScopedValue.where(CURRENT_USER, principal).call(action);
    }

    public static void runAs(UserPrincipal principal, Runnable action) {
        ScopedValue.where(CURRENT_USER, principal).run(action);
    }

    public static UserPrincipal getRequiredPrincipal() {
        if (!CURRENT_USER.isBound()) {
            throw new IllegalStateException("No principal bound in current execution scope");
        }
        return CURRENT_USER.get();
    }
}
