package lab.java25boot4.springsecurity;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Production coordinator using Java 25 ScopedValue for leak-proof, zero-overhead security context
 * propagation across concurrent virtual and platform threads.
 */
public final class ScopedSecurityContextCoordinator {

    public static final ScopedValue<SecurityUserContext> SECURITY_CONTEXT =
            ScopedValue.newInstance();

    private ScopedSecurityContextCoordinator() {}

    public record SecurityUserContext(String username, String role, Set<String> permissions) {
        public SecurityUserContext {
            permissions = Set.copyOf(permissions);
        }

        public boolean hasRole(String expectedRole) {
            String normalizedRole =
                    role.toUpperCase().startsWith("ROLE_") ? role.substring(5) : role;
            String normalizedExpected =
                    expectedRole.toUpperCase().startsWith("ROLE_")
                            ? expectedRole.substring(5)
                            : expectedRole;
            return normalizedRole.equalsIgnoreCase(normalizedExpected);
        }

        public boolean hasPermission(String expectedPermission) {
            return permissions.contains(expectedPermission);
        }
    }

    /** Executes a runnable within a strictly bounded security context. */
    public static void runWithContext(SecurityUserContext context, Runnable task) {
        ScopedValue.where(SECURITY_CONTEXT, context).run(task);
    }

    /** Executes a callable within a strictly bounded security context, returning a result. */
    public static <T> T callWithContext(SecurityUserContext context, Callable<T> task)
            throws Exception {
        return ScopedValue.where(SECURITY_CONTEXT, context).call(task::call);
    }

    /** Retrieves the current security context if bound in scope. */
    public static Optional<SecurityUserContext> currentContext() {
        return SECURITY_CONTEXT.isBound() ? Optional.of(SECURITY_CONTEXT.get()) : Optional.empty();
    }
}
