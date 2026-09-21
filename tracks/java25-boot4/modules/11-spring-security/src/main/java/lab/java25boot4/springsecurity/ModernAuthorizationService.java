package lab.java25boot4.springsecurity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Service demonstrating modern authorization enforcement and scoped context inspection. */
@Service
public class ModernAuthorizationService {

    public String performAdminOperation(String actionName) {
        ScopedSecurityContextCoordinator.SecurityUserContext context =
                ScopedSecurityContextCoordinator.currentContext()
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Unauthenticated: No active security context"));

        if (!context.hasRole("ROLE_ADMIN")) {
            throw new AccessDeniedException(
                    "Forbidden: User " + context.username() + " lacks required ADMIN role");
        }

        return "SUCCESS: Action '" + actionName + "' executed by " + context.username();
    }

    public boolean canAccessResource(String requiredPermission) {
        return ScopedSecurityContextCoordinator.currentContext()
                .map(ctx -> ctx.hasPermission(requiredPermission))
                .orElse(false);
    }
}
