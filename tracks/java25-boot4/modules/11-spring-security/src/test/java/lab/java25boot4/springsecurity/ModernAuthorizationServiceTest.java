package lab.java25boot4.springsecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ModernAuthorizationServiceTest {

    private final ModernAuthorizationService service = new ModernAuthorizationService();

    @Test
    @DisplayName("performAdminOperation succeeds when active context has ADMIN role")
    void performAdminOperationShouldSucceedForAdmin() {
        var adminContext =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "superadmin", "ROLE_ADMIN", Set.of("users:delete"));

        ScopedSecurityContextCoordinator.runWithContext(
                adminContext,
                () -> {
                    String result = service.performAdminOperation("purge-cache");
                    assertThat(result)
                            .isEqualTo("SUCCESS: Action 'purge-cache' executed by superadmin");
                });
    }

    @Test
    @DisplayName("performAdminOperation throws AccessDeniedException when context lacks ADMIN role")
    void performAdminOperationShouldFailForNonAdmin() {
        var userContext =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "regular_user", "ROLE_USER", Set.of());

        ScopedSecurityContextCoordinator.runWithContext(
                userContext,
                () -> {
                    assertThatThrownBy(() -> service.performAdminOperation("purge-cache"))
                            .isInstanceOf(AccessDeniedException.class)
                            .hasMessageContaining("Forbidden");
                });
    }

    @Test
    @DisplayName("performAdminOperation throws AccessDeniedException when unauthenticated")
    void performAdminOperationShouldFailWhenUnauthenticated() {
        assertThatThrownBy(() -> service.performAdminOperation("purge-cache"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthenticated");
    }

    @Test
    @DisplayName("canAccessResource evaluates permissions accurately from context")
    void canAccessResourceShouldEvaluatePermissions() {
        var userContext =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "editor", "ROLE_EDITOR", Set.of("articles:publish"));

        ScopedSecurityContextCoordinator.runWithContext(
                userContext,
                () -> {
                    assertThat(service.canAccessResource("articles:publish")).isTrue();
                    assertThat(service.canAccessResource("system:shutdown")).isFalse();
                });

        assertThat(service.canAccessResource("articles:publish")).isFalse();
    }
}
