package lab.java25boot4.springsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScopedSecurityContextCoordinatorTest {

    @Test
    @DisplayName("Context is empty outside of ScopedValue.where block")
    void contextShouldBeEmptyOutsideScope() {
        assertThat(ScopedSecurityContextCoordinator.currentContext()).isEmpty();
    }

    @Test
    @DisplayName("runWithContext binds security context strictly within execution block")
    void runWithContextShouldBindAndIsolateContext() {
        var context =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "sarah", "ROLE_ADMIN", Set.of("audit:read", "audit:write"));

        AtomicBoolean executed = new AtomicBoolean(false);

        ScopedSecurityContextCoordinator.runWithContext(
                context,
                () -> {
                    var current = ScopedSecurityContextCoordinator.currentContext();
                    assertThat(current).isPresent();
                    assertThat(current.get().username()).isEqualTo("sarah");
                    assertThat(current.get().hasRole("ADMIN")).isTrue();
                    assertThat(current.get().hasPermission("audit:write")).isTrue();
                    executed.set(true);
                });

        assertThat(executed.get()).isTrue();
        assertThat(ScopedSecurityContextCoordinator.currentContext()).isEmpty();
    }

    @Test
    @DisplayName("callWithContext returns computed value and clears context on exit")
    void callWithContextShouldReturnResultAndClearContext() throws Exception {
        var context =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "alex", "USER", Set.of("profile:read"));

        String result =
                ScopedSecurityContextCoordinator.callWithContext(
                        context,
                        () -> {
                            var current = ScopedSecurityContextCoordinator.currentContext();
                            return current.map(
                                            ScopedSecurityContextCoordinator.SecurityUserContext
                                                    ::username)
                                    .orElse("anonymous");
                        });

        assertThat(result).isEqualTo("alex");
        assertThat(ScopedSecurityContextCoordinator.currentContext()).isEmpty();
    }

    @Test
    @DisplayName("Virtual threads isolate security context without cross-thread contamination")
    void virtualThreadsShouldIsolateContext() throws Exception {
        var adminCtx =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "admin-vt", "ADMIN", Set.of("admin:all"));
        var guestCtx =
                new ScopedSecurityContextCoordinator.SecurityUserContext(
                        "guest-vt", "GUEST", Set.of());

        Thread vt1 =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    ScopedSecurityContextCoordinator.runWithContext(
                                            adminCtx,
                                            () -> {
                                                try {
                                                    Thread.sleep(20);
                                                } catch (InterruptedException ignored) {
                                                }
                                                assertThat(
                                                                ScopedSecurityContextCoordinator
                                                                        .currentContext()
                                                                        .orElseThrow()
                                                                        .username())
                                                        .isEqualTo("admin-vt");
                                            });
                                });

        Thread vt2 =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    ScopedSecurityContextCoordinator.runWithContext(
                                            guestCtx,
                                            () -> {
                                                assertThat(
                                                                ScopedSecurityContextCoordinator
                                                                        .currentContext()
                                                                        .orElseThrow()
                                                                        .username())
                                                        .isEqualTo("guest-vt");
                                            });
                                });

        vt1.join();
        vt2.join();

        assertThat(ScopedSecurityContextCoordinator.currentContext()).isEmpty();
    }
}
